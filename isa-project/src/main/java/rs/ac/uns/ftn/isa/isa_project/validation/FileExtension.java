package rs.ac.uns.ftn.isa.isa_project.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FileExtensionValidator.class)
public @interface FileExtension {
    String[] allowed();
    String message() default "Nepodržana ekstenzija";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}