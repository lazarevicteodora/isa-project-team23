package rs.ac.uns.ftn.isa.isa_project.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class FileSizeValidator implements ConstraintValidator<FileSize, MultipartFile> {
    private long maxSize;

    @Override
    public void initialize(FileSize constraintAnnotation) {
        this.maxSize = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) return true;
        return file.getSize() <= maxSize;
    }
}
