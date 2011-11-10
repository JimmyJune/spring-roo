package org.springframework.roo.addon.jsf.converter;

import static java.lang.reflect.Modifier.PUBLIC;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.CONVERTER;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.FACES_CONTEXT;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.FACES_CONVERTER;
import static org.springframework.roo.addon.jsf.model.JsfJavaType.UI_COMPONENT;
import static org.springframework.roo.model.JavaType.OBJECT;

import java.util.Arrays;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeIdentifierNamingUtils;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.details.MethodMetadata;
import org.springframework.roo.classpath.details.MethodMetadataBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotatedJavaType;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.itd.AbstractItdTypeDetailsProvidingMetadataItem;
import org.springframework.roo.classpath.itd.InvocableMemberBodyBuilder;
import org.springframework.roo.classpath.layers.MemberTypeAdditions;
import org.springframework.roo.metadata.MetadataIdentificationUtils;
import org.springframework.roo.model.ImportRegistrationResolver;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.ContextualPath;
import org.springframework.roo.support.style.ToStringCreator;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.StringUtils;

/**
 * Metadata for {@link RooJsfConverter}.
 *
 * @author Alan Stewart
 * @since 1.2.0
 */
public class JsfConverterMetadata extends AbstractItdTypeDetailsProvidingMetadataItem {

	// Constants
	private static final String PROVIDES_TYPE_STRING = JsfConverterMetadata.class.getName();
	private static final String PROVIDES_TYPE = MetadataIdentificationUtils.create(PROVIDES_TYPE_STRING);

	// Fields
	private JavaType entity;

	public JsfConverterMetadata(final String identifier, final JavaType aspectName, final PhysicalTypeMetadata governorPhysicalTypeMetadata, final JsfConverterAnnotationValues annotationValues, final MemberTypeAdditions findAllMethod) {
		super(identifier, aspectName, governorPhysicalTypeMetadata);
		Assert.isTrue(isValid(identifier), "Metadata identification string '" + identifier + "' is invalid");
		Assert.notNull(annotationValues, "Annotation values required");

		if (!isValid()) {
			return;
		}

		this.entity = annotationValues.getEntity();

		if (findAllMethod == null) {
			valid = false;
			return;
		}
		if (!isConverterInterfaceIntroduced()) {
			final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
			imports.addImport(CONVERTER);
			builder.addImplementsType(CONVERTER);
		}

		builder.addAnnotation(getFacesConverterAnnotation());
		builder.addMethod(getGetAsObjectMethod(findAllMethod));
		builder.addMethod(getGetAsStringMethod(findAllMethod));

		// Create a representation of the desired output ITD
		itdTypeDetails = builder.build();
	}

	private AnnotationMetadata getFacesConverterAnnotation() {
		AnnotationMetadata annotation = getTypeAnnotation(FACES_CONVERTER);
		if (annotation == null) {
			return null;
		}

		AnnotationMetadataBuilder annotationBuulder = new AnnotationMetadataBuilder(annotation);
		// annotationBuulder.addClassAttribute("forClass", entity); // TODO The forClass attribute causes issues
		annotationBuulder.addStringAttribute("value", destination.getFullyQualifiedTypeName());
		return annotationBuulder.build();
	}

	private boolean isConverterInterfaceIntroduced() {
		return isImplementing(governorTypeDetails, CONVERTER);
	}

	private MethodMetadata getGetAsObjectMethod(final MemberTypeAdditions findAllMethod) {
		final JavaSymbolName methodName = new JavaSymbolName("getAsObject");
		final JavaType[] parameterTypes = { FACES_CONTEXT, UI_COMPONENT, JavaType.STRING };
		if (governorHasMethod(methodName, parameterTypes)) {
			return null;
		}

		findAllMethod.copyAdditionsTo(builder, governorTypeDetails);

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(entity);
		imports.addImport(FACES_CONTEXT);
		imports.addImport(UI_COMPONENT);

		String simpleTypeName = entity.getSimpleTypeName();

		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("if (value == null) {");  
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return null;");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("for (" + simpleTypeName + " " + StringUtils.uncapitalize(simpleTypeName) + " : " + findAllMethod.getMethodCall() + ") {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("if (" + StringUtils.uncapitalize(simpleTypeName) + ".toString().equals(value)) {");
		bodyBuilder.indent();
		bodyBuilder.appendFormalLine("return " + StringUtils.uncapitalize(simpleTypeName) + ";");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.indentRemove();
		bodyBuilder.appendFormalLine("}");
		bodyBuilder.appendFormalLine("return null;");

		// Create getAsObject method
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("context"), new JavaSymbolName("component"), new JavaSymbolName("value"));
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, OBJECT, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	private MethodMetadata getGetAsStringMethod(final MemberTypeAdditions findAllMethod) {
		final JavaSymbolName methodName = new JavaSymbolName("getAsString");
		final JavaType[] parameterTypes = { FACES_CONTEXT, UI_COMPONENT, OBJECT };
		if (governorHasMethod(methodName, parameterTypes)) {
			return null;
		}

		final ImportRegistrationResolver imports = builder.getImportRegistrationResolver();
		imports.addImport(entity);
		imports.addImport(FACES_CONTEXT);
		imports.addImport(UI_COMPONENT);

		String simpleTypeName = entity.getSimpleTypeName();
		
		InvocableMemberBodyBuilder bodyBuilder = new InvocableMemberBodyBuilder();
		bodyBuilder.appendFormalLine("return value instanceof " + simpleTypeName + " ? ((" + simpleTypeName + ") value).toString() : \"\";");  

		// Create getAsString method
		final List<JavaSymbolName> parameterNames = Arrays.asList(new JavaSymbolName("context"), new JavaSymbolName("component"), new JavaSymbolName("value"));
		final MethodMetadataBuilder methodBuilder = new MethodMetadataBuilder(getId(), PUBLIC, methodName, JavaType.STRING, AnnotatedJavaType.convertFromJavaTypes(parameterTypes), parameterNames, bodyBuilder);
		return methodBuilder.build();
	}

	@Override
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		tsc.append("identifier", getId());
		tsc.append("valid", valid);
		tsc.append("aspectName", aspectName);
		tsc.append("destinationType", destination);
		tsc.append("governor", governorPhysicalTypeMetadata.getId());
		tsc.append("itdTypeDetails", itdTypeDetails);
		return tsc.toString();
	}

	public static String getMetadataIdentiferType() {
		return PROVIDES_TYPE;
	}

	public static String createIdentifier(final JavaType javaType, final ContextualPath path) {
		return PhysicalTypeIdentifierNamingUtils.createIdentifier(PROVIDES_TYPE_STRING, javaType, path);
	}

	public static JavaType getJavaType(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getJavaType(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static ContextualPath getPath(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.getPath(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}

	public static boolean isValid(final String metadataIdentificationString) {
		return PhysicalTypeIdentifierNamingUtils.isValid(PROVIDES_TYPE_STRING, metadataIdentificationString);
	}
}
