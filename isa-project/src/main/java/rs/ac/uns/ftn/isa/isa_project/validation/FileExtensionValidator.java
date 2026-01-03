package rs.ac.uns.ftn.isa.isa_project.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;
import java.util.Arrays;
import java.util.List;

public class FileExtensionValidator implements ConstraintValidator<FileExtension, MultipartFile> {
    private List<String> allowedExtensions;

    @Override
    public void initialize(FileExtension constraintAnnotation) {
        this.allowedExtensions = Arrays.asList(constraintAnnotation.allowed());
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) return true;

        String filename = file.getOriginalFilename();
        if (filename == null) return false;

        String extension = "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = filename.substring(lastDot + 1).toLowerCase();
        }

        return allowedExtensions.contains(extension);
    }
}
