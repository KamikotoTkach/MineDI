package ru.cwcode.tkach.minedi.annotation;

import java.lang.annotation.*;

@BaseAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Component {

}
