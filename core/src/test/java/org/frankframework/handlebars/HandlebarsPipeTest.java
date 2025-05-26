package org.frankframework.handlebars;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterType;
import org.frankframework.pipes.DataSonnetPipe;
import org.frankframework.pipes.HandlebarsPipe;

import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassLoaderUtils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class HandlebarsPipeTest extends PipeTestBase<HandlebarsPipe> {

	private final String EXAMPLE_INPUT = "<root><name>John</name><surname>Doe</surname></root>";

	@Override
	public HandlebarsPipe createPipe() throws ConfigurationException {
		HandlebarsPipe pipe = new HandlebarsPipe();
		HandlebarsEngine engine = new HandlebarsEngine();
		engine.afterPropertiesSet();
		pipe.setHandlebarsEngine(engine);
		pipe.addForward(new PipeForward("success", null));

		return pipe;
	}

	@Test
	public void testHandlebarsResolvesFieldsWhenXmlRootTagIsRoot() throws Exception {
		// Arrange: Use the apilistener.hbs template (expects flat context)
		pipe.setTemplateFile("/apilistener.hbs");
		pipe.setUseInputAsContext(true);

		// Input where root tag is <root> — previously caused collisions
		String inputXml =
				"<root>" +
					"<name>MyApi</name>" +
					"<method>GET</method>" +
					"<uriPattern>/test</uriPattern>" +
					"<headerParams>X-Test:123</headerParams>" +
					"<produces>application/json</produces>" +
					"<consumes>application/xml</consumes>" +
				"</root>";

		// Expected output based on apilistener.hbs template
		String expectedOutput =
				"<ApiListener\n" +
						"\tname=\"John\"\n" +
						"\tmethod=\"GET\"\n" +
						"\turiPattern=\"/test\"\n" +
						"\theaderParams=\"X-Test:123\"\n" +
						"\tproduces=\"application/json\"\n" +
						"\tconsumes=\"application/xml\"\n" +
						"/>\r\n";

		String paramInput = "<root><name>John</name><description>Developer</description></root>";

		Parameter descriptionParam = new Parameter("description", paramInput);
		descriptionParam.setType(ParameterType.DOMDOC);
		pipe.addParameter(descriptionParam);

		// Act
		pipe.configure();
		PipeRunResult result = doPipe(inputXml);
		String actualOutput = result.getResult().asString();

		// Assert: Ensure correct context resolution
		Assertions.assertNotNull(actualOutput, "Output should not be null");

		expectedOutput = expectedOutput.replace("\r\n", "\n");
		actualOutput = actualOutput.replace("\r\n", "\n");
		Assertions.assertEquals(expectedOutput, actualOutput,
				"When XML root tag is <root>, template should still resolve fields like {{name}} without needing root.root.name");
	}

	@Test
	public void testHandlebarsTemplateWithDomDocParameter() throws Exception {
		// Arrange
		pipe.setTemplateFile("/test-template.hbs");
		pipe.setUseInputAsContext(true);

		String inputXml = "<root>\n\t<name>John</name>\n\t<description>Developer</description>\n</root>\n";

		Parameter descriptionParam = new Parameter("description", inputXml);
		descriptionParam.setType(ParameterType.DOMDOC);
		pipe.addParameter(descriptionParam);

		// Act
		pipe.configure();
		PipeRunResult result = doPipe(inputXml);
		String actualResult = result.getResult().asString();

		// Assert (normalize XML)
		Diff diff = DiffBuilder.compare(inputXml)
				.withTest(actualResult)
				.ignoreWhitespace()
				.ignoreComments()
				.checkForSimilar()
				.build();

		Assertions.assertFalse(diff.hasDifferences(), () -> "XML output differs: " + diff.toString());
	}

	@Test
	public void testOpenApiSpecWithTemplateProcessing() throws ConfigurationException, PipeRunException, IOException, URISyntaxException {
		// Arrange: Load OpenAPI YAML as input
		pipe.setTemplateFile("/apilistener.hbs");
		pipe.setUseInputAsContext(true);

		Path yamlPath = Paths.get(
				getClass().getClassLoader().getResource("openapi.yaml").toURI()
		);
		String openApiContent = Files.readString(yamlPath);

		// Define expected XML output from the template
		String expectedXml =
				"<ApiListener\n" +
					"\tname=\"\"\n" +
					"\tmethod=\"\"\n" +
					"\turiPattern=\"\"\n" +
					"\theaderParams=\"\"\n" +
					"\tproduces=\"\"\n" +
					"\tconsumes=\"\"\n" +
					"/>\n";

		// Act
		pipe.configure();
		PipeRunResult result = doPipe(openApiContent);
		String actualXml = result.getResult().asString();

		// Assert (normalize XML)
		Diff diff = DiffBuilder.compare(actualXml)
				.withTest(expectedXml)
				.ignoreWhitespace()
				.ignoreComments()
				.checkForSimilar()
				.build();

		Assertions.assertFalse(diff.hasDifferences(), () -> "XML output differs: " + diff.toString());
	}
}
