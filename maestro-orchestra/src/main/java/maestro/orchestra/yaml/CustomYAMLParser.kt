package maestro.orchestra.yaml

import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.core.io.IOContext
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLParser
import org.yaml.snakeyaml.LoaderOptions
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.io.StringReader

class CustomYAMLFactory : YAMLFactory() {
    override fun createParser(inputStream: InputStream): YAMLParser {
        val ctxt = _createContext(_createContentReference(inputStream), false)

        return CustomYAMLParser(
            ctxt,
            _parserFeatures,
            _yamlParserFeatures,
            _loaderOptions,
            _objectCodec,
            _createReader(inputStream, null, ctxt)
        )
    }

    override fun createParser(content: String): YAMLParser {
        return createParser(StringReader(content))
    }

    override fun createParser(r: Reader): YAMLParser {
        val ctxt = _createContext(_createContentReference(r), false)
        return _createParser(_decorate(r, ctxt), ctxt)
    }

    override fun _createParser(r: Reader, ctxt: IOContext): YAMLParser {
        return CustomYAMLParser(
            ctxt, _parserFeatures, _yamlParserFeatures,
            _loaderOptions, _objectCodec, r
        )
    }
}

class CustomYAMLParser(
    ctxt: IOContext?,
    parserFeatures: Int,
    formatFeatures: Int,
    loaderOptions: LoaderOptions?,
    codec: ObjectCodec?,
    reader: Reader?) : YAMLParser(ctxt, parserFeatures, formatFeatures, loaderOptions, codec, reader) {
        override fun _matchYAMLBoolean(value: String, len: Int): Boolean? {
            if (value.lowercase() == "true") return true
            if (value.lowercase() == "false") return false
            return null
        }
}