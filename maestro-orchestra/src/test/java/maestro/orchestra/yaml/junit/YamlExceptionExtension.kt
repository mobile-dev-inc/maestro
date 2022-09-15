package maestro.orchestra.yaml.junit

import com.google.common.truth.Truth.assertThat
import maestro.orchestra.yaml.YamlCommandReader
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import java.nio.file.Paths

class YamlExceptionExtension : ParameterResolver {

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean {
        return RuntimeException::class.java.isAssignableFrom(parameterContext.parameter.type)
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Any {
        val yamlFileAnnotation = parameterContext.findAnnotation(YamlFile::class.java)
            .orElseThrow { IllegalArgumentException("No @YamlFile annotation found") }

        val resource = this::class.java.getResource("/YamlCommandReaderTest/${yamlFileAnnotation.name}")!!
        val resourceFile = Paths.get(resource.toURI())

        return try {
            YamlCommandReader.readCommands(resourceFile)
        } catch (e: Throwable) {
            assertThat(e).isInstanceOf(parameterContext.parameter.type)
            return e
        }
    }
}
