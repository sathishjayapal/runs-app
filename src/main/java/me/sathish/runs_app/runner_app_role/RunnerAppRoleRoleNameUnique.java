package me.sathish.runs_app.runner_app_role;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import org.springframework.web.servlet.HandlerMapping;


/**
 * Validate that the roleName value isn't taken yet.
 */
@Target({ FIELD, METHOD, ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(
        validatedBy = RunnerAppRoleRoleNameUnique.RunnerAppRoleRoleNameUniqueValidator.class
)
public @interface RunnerAppRoleRoleNameUnique {

    String message() default "{exists.runnerAppRole.roleName}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class RunnerAppRoleRoleNameUniqueValidator implements ConstraintValidator<RunnerAppRoleRoleNameUnique, String> {

        private final RunnerAppRoleService runnerAppRoleService;
        private final HttpServletRequest request;

        public RunnerAppRoleRoleNameUniqueValidator(final RunnerAppRoleService runnerAppRoleService,
                final HttpServletRequest request) {
            this.runnerAppRoleService = runnerAppRoleService;
            this.request = request;
        }

        @Override
        public boolean isValid(final String value, final ConstraintValidatorContext cvContext) {
            if (value == null) {
                // no value present
                return true;
            }
            @SuppressWarnings("unchecked") final Map<String, String> pathVariables =
                    ((Map<String, String>)request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
            final String currentId = pathVariables.get("id");
            if (currentId != null && value.equalsIgnoreCase(runnerAppRoleService.get(Long.parseLong(currentId)).getRoleName())) {
                // value hasn't changed
                return true;
            }
            return !runnerAppRoleService.roleNameExists(value);
        }

    }

}
