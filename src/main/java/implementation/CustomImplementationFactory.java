package implementation;

import interpreter.PrintScriptFormatter;
import interpreter.PrintScriptInterpreter;
import interpreter.PrintScriptLinter;

import java.io.InputStream;
import java.io.Writer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.printscript.common.Version;
import org.printscript.common.Result;
import org.printscript.common.Success;

import org.printscript.lexer.config.LexerFactory;
import org.printscript.token.TokenStream;

import org.printscript.formatter.Formatter;
import org.printscript.formatter.config.FormatterOptions;
import org.printscript.formatter.config.FormatterConfig;
import org.printscript.formatter.config.ExternalFormatterConfigLoader;
import org.printscript.formatter.factories.GlobalFormatterFactory;

public class CustomImplementationFactory implements PrintScriptFactory {

    // --- Helpers ------------------------------------------------------------

    private static Version parseVersion(String v) {
        if ("1.0".equals(v)) return Version.V0;
        if ("1.1".equals(v)) return Version.V1;
        throw new IllegalArgumentException("Unsupported version: " + v);
    }

    private static FormatterOptions loadOptions(InputStream config) {
        try {
            if (config == null) return new FormatterConfig();
            byte[] cfgBytes = config.readAllBytes();
            if (cfgBytes.length == 0) return new FormatterConfig();
            return ExternalFormatterConfigLoader.INSTANCE.load(cfgBytes);
        } catch (Exception e) {
            // Si el JSON viene mal o hay IO, seguimos con defaults.
            return new FormatterConfig();
        }
    }

    // --- Interpreter (no-op por ahora) -------------------------------------

    @Override
    public PrintScriptInterpreter interpreter() {
        return (src, version, emitter, handler, provider) -> {
            // no-op: enfocate en formatter primero
        };
    }

    // --- Formatter (con manejo de IOException y Result con genéricos) -------

    @Override
    public PrintScriptFormatter formatter() {
        return (InputStream src, String version, InputStream config, Writer out) -> {
            String code;
            try {
                code = new String(src.readAllBytes(), StandardCharsets.UTF_8); // puede tirar IOException
            } catch (IOException e) {
                // Si no puedo leer el source, escribo vacío y salgo.
                try { out.write(""); } catch (IOException ignored) {}
                return;
            }

            Version v = parseVersion(version);

            // Lexer → tokens
            LexerFactory lexerFactory = new LexerFactory();
            TokenStream tokens = lexerFactory.tokenStream(v, code);

            // Opciones desde config (InputStream)
            FormatterOptions options = loadOptions(config);

            // Obtener formatter para la versión (Kotlin object → .INSTANCE)
            Formatter fmt = GlobalFormatterFactory.INSTANCE.forVersion(v, options);
            if (fmt == null) {
                try { out.write(code); } catch (IOException ignored) {}
                return;
            }

            StringBuilder sb = new StringBuilder();
            // Result<kotlin.Unit, LabeledError> → en Java uso comodines
            Result<?, ?> result = fmt.format(tokens, sb);

            try {
                if (result instanceof Success<?>) {
                    out.write(sb.toString());        // también puede tirar IOException
                } else {
                    out.write(code);                 // fallback si hubo error
                }
            } catch (IOException ignored) {
                // No podemos propagar IOException; silencioso para no romper tests
            }
        };
    }

    // --- Linter (no-op) -----------------------------------------------------

    @Override
    public PrintScriptLinter linter() {
        // your PrintScript linter should be returned here.
        // make sure to ADAPT your linter to PrintScriptLinter interface.
        throw new NotImplementedException("Needs implementation"); // TODO: implement
    }
}
