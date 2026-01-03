package rs.ac.uns.ftn.isa.isa_project.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FileSizeValidator.class)
public @interface FileSize {
    long max() default Long.MAX_VALUE;
    String message() default "Fajl je prevelik";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
