package ru.cwcode.tkach.minedi.annotation;

import java.lang.annotation.*;

@BaseAnnotation
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {
}
