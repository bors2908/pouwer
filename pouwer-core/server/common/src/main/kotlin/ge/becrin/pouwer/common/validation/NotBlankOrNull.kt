package ge.becrin.pouwer.common.validation

import jakarta.validation.Constraint
import kotlin.reflect.KClass
import org.hibernate.validator.constraints.CompositionType
import org.hibernate.validator.constraints.ConstraintComposition

@ConstraintComposition(CompositionType.OR)
@Constraint(validatedBy = [])
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotBlankOrNull(
    val message: String = "The value must be null or not blank.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<*>> = []
)
