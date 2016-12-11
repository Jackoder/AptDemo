package com.example;

import com.example.ann.Builder;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

@AutoService(Processor.class)
public class AutoBuilderProcessor extends AbstractProcessor {

    public static final String SUFFIX = "Builder";

    Elements mElementUtils;
    Filer    mFiler;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mElementUtils = processingEnvironment.getElementUtils();
        mFiler = processingEnvironment.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(Builder.class);
        for (Element element : elements) {
            if (element.getKind() == ElementKind.CLASS) {
                List<Element> fields = new ArrayList<>();
                List<? extends Element> enclosedElements = element.getEnclosedElements();
                for (Element enclosedElement : enclosedElements) {
                    if (enclosedElement.getKind() == ElementKind.FIELD &&
                            !enclosedElement.getModifiers().contains(Modifier.PRIVATE)) {
                        fields.add(enclosedElement);
                    }
                }
                ClassName className = ClassName.get(
                        mElementUtils.getPackageOf(element).getQualifiedName().toString(),
                        element.getSimpleName().toString() + SUFFIX);
                TypeSpec.Builder typeSpec = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
                for (Element field : fields) {
                    typeSpec.addField(FieldSpec.builder(
                            TypeName.get(field.asType()), field.getSimpleName().toString(), Modifier.PRIVATE).
                            build());
                }
                MethodSpec methodBuilder = MethodSpec.methodBuilder("builder")
                        .addModifiers(Modifier.PUBLIC)
                        .addModifiers(Modifier.STATIC)
                        .returns(className)
                        .addStatement("return new $T()", className).build();
                typeSpec.addMethod(methodBuilder);
                for (Element field : fields) {
                    typeSpec.addMethod(MethodSpec.methodBuilder(field.getSimpleName().toString())
                            .addModifiers(Modifier.PUBLIC)
                            .returns(className)
                            .addParameter(TypeName.get(field.asType()), field.getSimpleName().toString())
                            .addStatement("this.$N = $N", field.getSimpleName().toString(), field.getSimpleName().toString())
                            .addStatement("return this")
                            .build());
                }

                MethodSpec.Builder methodSpec = MethodSpec.methodBuilder("build")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.get(element.asType()));
                methodSpec.addStatement("$T item = new $T()", TypeName.get(element.asType()), TypeName.get(element.asType()));
                for (Element field : fields) {
                    methodSpec.addStatement("this.$N = $N", field.getSimpleName().toString(), field.getSimpleName().toString());
                }
                methodSpec.addStatement("return item");
                typeSpec.addMethod(methodSpec.build());

                JavaFile javaFile = JavaFile.builder(
                        mElementUtils.getPackageOf(element).getQualifiedName().toString(), typeSpec.build()).build();
                try {
                    javaFile.writeTo(mFiler);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new IllegalStateException("Write failed.");
                }
            }
        }
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(Builder.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }
}
