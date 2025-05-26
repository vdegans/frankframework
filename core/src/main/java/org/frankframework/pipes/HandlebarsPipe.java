/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.pipes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.collection.CollectionException;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISenderWithParameters;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;

import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.FixedForwardPipe;

import org.frankframework.stream.Message;

import org.springframework.messaging.support.MessageBuilder;
import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.github.jknack.handlebars.Template;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.parser.OpenAPIV3Parser;
import lombok.Getter;
import lombok.Setter;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.Adapter;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.Resource;
import org.frankframework.handlebars.DomdocValueResolver;
import org.frankframework.handlebars.HandlebarsEngine;
import org.frankframework.handlebars.ParameterListValueResolver;
import org.frankframework.http.rest.ApiListener;
import org.frankframework.http.rest.ApiListener.HttpMethod;
import org.frankframework.http.rest.MediaTypes;
import org.frankframework.parameters.Parameter;
import org.frankframework.receivers.Receiver;
import org.frankframework.stream.Message;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.FileUtils;
import org.frankframework.util.Misc;
import org.frankframework.util.XmlUtils;

/**
 * Replaces all occurrences of one string with another.
 *
 * @author Gerrit van Brakel
 * @since 4.2
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class HandlebarsPipe extends FixedForwardPipe {
	private @Getter String templateFile = null;
	private @Getter String templateName = null;
	private @Getter String templateNameSessionKey = null;

	private @Getter boolean useInputAsContext = true;

	private @Getter String contextSessionKey = null;
	private @Getter String contextParamName = "context";
	private IParameter contextParameter = null;

	private @Getter @Setter HandlebarsEngine handlebarsEngine = null;
	private @Getter Template template = null;
	private @Getter Template configurationTemplate = null;
	private @Getter String openApiFilename = null;
	OpenAPI openAPI = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if(StringUtils.isEmpty(getTemplateFile()) &&
				StringUtils.isEmpty(getTemplateName()) &&
				StringUtils.isEmpty(getTemplateNameSessionKey()))
			throw new ConfigurationException("a template must be specified with one of [templateFile, templateName, templateNameSessionKey]");

		if(!Misc.exclusiveOr(
				StringUtils.isNotEmpty(getTemplateFile()),
				StringUtils.isNotEmpty(getTemplateName()),
				StringUtils.isNotEmpty(getTemplateNameSessionKey()))){
			throw new ConfigurationException("only one of [templateFile, templateName, templateNameSessionKey] can be configured");
		}

		if(StringUtils.isNotEmpty(getTemplateFile())){
			try {
				Resource templateResource = Resource.getResource(this, getTemplateFile());
				template = handlebarsEngine.compile(templateResource);
			} catch (IOException e) {
				throw new ConfigurationException("unable to load template file ["+getTemplateFile()+"]", e);
			}
		}

		if(StringUtils.isNotEmpty(getTemplateName())){
			try {
				template = handlebarsEngine.compile(getTemplateName());
			} catch (IOException e) {
				throw new ConfigurationException("unable to load template with name ["+getTemplateName()+"]", e);
			}
		}

		if (StringUtils.isNotEmpty(getContextParamName())) {
			if (getParameterList() != null) {
				contextParameter = getParameterList().findParameter(getContextParamName());
			}
			if (contextParameter == null) {
				ConfigurationWarnings.add(this, log, "contextParameter ["+getContextParamName()+"] not found");
			}
		}

//		try {
//			Context context =  buildContextFromParameters(null, null);
//		} catch (PipeRunException | ParameterException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		if (StringUtils.isEmpty(getOpenApiFilename()))
//			throw new ConfigurationException("openApiFilename must be specified");
//
//		try {
//			Resource oasFile = Resource.getResource(this, getOpenApiFilename());
//
//			String yaml = new BufferedReader(
//					new InputStreamReader(oasFile.openStream(), StandardCharsets.UTF_8))
//						.lines()
//						.collect(Collectors.joining("\n"));
//			openAPI = new OpenAPIV3Parser().readContents(yaml).getOpenAPI();
//		} catch (IOException e) {
//			throw new ConfigurationException("could not find file ["+getOpenApiFilename()+"]",e);
//		}
//
//		if(openAPI == null) {
//			throw new ConfigurationException("could not find file ["+getOpenApiFilename()+"]");
//		}
	}

	public static final class Builder {
		private Context.Builder contextBuilder;

		private Builder() {
			contextBuilder = Context.newBuilder(new HashMap<String, Object>());
		}

		/**
	     * With attribute.
	     *
	     * @param name The attribute's name. Required.
	     * @param value The attribute's value.
	     * @return This builder.
	     */
		public Builder withAttribute(final String name, final Object value) {
			contextBuilder.combine(name, value);

			return this;
		}

		/**
	     * With all map entries.
	     *
	     * @param value The Map data. Required.
	     * @return This builder.
	     */
		public Builder withMap(final Map<String, ?> value) {
			contextBuilder.combine(value);

			return this;
		}

		/**
	     * Build a context stack.
	     *
	     * @return A new context stack.
	     */
		public Context build() {
			return contextBuilder.build();
		}
	}

	/**
	  * Start a new context builder.
	  *
	  * @return A new context builder.
	  */
	public Builder newBuilder() {
		return new Builder();
	}

//	protected void addToContext(Message message, PipeLineSession session, Context context) throws IOException {
//		if(StringUtils.isEmpty(message.asString())){
//			return;
//		}
//
//		JsonNode node = null;
//		try {
//			node = new ObjectMapper().readValue(message.asInputStream(), JsonNode.class);
//
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
////		Resource xml = Resource.getResource(this, "xmlInput.xml");
////		MapCollector mapCollector = new MapCollector();
////		try {
////			XmlUtils.parseXml(xml.asInputSource(), mapCollector);
////
////			} catch (IOException | SAXException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		}
//		//Map<String, String> contextMap = mapCollector.result;
//
//		Context ctx = Context.newBuilder(null)
//				.resolver(JsonNodeValueResolver.INSTANCE)
//				.build();
//	}
//
////	protected Context buildContextFromParameters(Message message, PipeLineSession session) throws PipeRunException, ParameterException, CollectionException {
////		this.getParameterValueList(message, session);
////		Map<String, Object> parameterContextMap = new HashMap<String, Object>();
////		for (IParameter param : this.getParameterList()) {
////			Object valueObject = param.getValue(this.getParameterValueList(message, session), message, session, false);
////			if(log.isTraceEnabled()) log.trace("setting contextParameter ["+param.getName()+"] to "+valueObject);
////			parameterContextMap.put(param.getName(), valueObject);
////		}
////
////		return Context.newContext(parameterContextMap);
////	}
//
//	protected Context buildContextFromCode() {
//		Receiver receiver = new Receiver();
//		receiver.setName("receiverName");
//		ApiListener apiListener = new ApiListener();
//		apiListener.setName("apiListenerName");
//		apiListener.setUriPattern("/zaken");
//		apiListener.setMethod(HttpMethod.valueOf("GET"));
//		apiListener.setOperationId("get_zaken");
//		apiListener.setProduces(MediaTypes.fromValue("application/json"));
//		receiver.setListener(apiListener);
//
////		return Context.newBuilder(receiver)
////				.combine("receiver", receiver)
////				.combine("apiListener", apiListener)
////				.build();
//		return Context.newContext(receiver);
//	}
//
//	private List<Adapter> createConfiguration(OpenAPI openAPI) throws IOException, ConfigurationException {
//		Paths paths = openAPI.getPaths();
//		List<Adapter> adapters = new ArrayList<>();
//
//		String configurationName = openAPI.getInfo().getTitle() + " - " + openAPI.getInfo().getVersion();
//		Configuration configuration = new Configuration();
//		configuration.setDisplayName(configurationName);
//		configuration.setName(configurationName);
//
//		try {
//			for (Map.Entry<String, PathItem> path : paths.entrySet()) {
//				for (Map.Entry<PathItem.HttpMethod, Operation> operation : path.getValue().readOperationsMap().entrySet()) {
//					if(operation.getKey()!= PathItem.HttpMethod.HEAD) {
//						adapters.add(createEndpointAdapter(path.getKey(), path.getValue(), operation.getKey(), operation.getValue()));
//					}
//				}
//			}
//		} catch (Exception e) {
//			throw new ConfigurationException("Exception on generating openAPI adapters", e);
//		}
//
//		return adapters;
//	}

//	private Adapter createEndpointAdapter(String path, PathItem pathItem, PathItem.HttpMethod method, Operation operation) throws ConfigurationException {
//		Adapter adapter = new Adapter();
//		String name = path.substring(1).replace("/", "-") + "-" + method.toString();
//		adapter.setName(operation.getOperationId());
//		Receiver receiver = new Receiver();
//		receiver.setName(operation.getOperationId());
//		ApiListener apiListener = new ApiListener();
//		apiListener.setName(operation.getOperationId());
//		apiListener.setUriPattern(path);
//		apiListener.setMethod(HttpMethod.valueOf(method.toString()));
//		apiListener.setOperationId(operation.getOperationId());
//		if(apiListener.getMethod() != HttpMethod.GET && apiListener.getMethod() != HttpMethod.DELETE) {
////			String mediaType = operation.getRequestBody().getContent().values().toArray()[0].toString();
////			apiListener.setConsumes(MediaTypes.fromValue(mediaType));
//			apiListener.setConsumes(MediaTypes.fromValue("application/json"));
//		}
//		apiListener.setProduces(MediaTypes.fromValue("application/json"));
//		receiver.setListener(apiListener);
//		adapter.registerReceiver(receiver);
//
//		PipeLine pipeLine = new PipeLine();
//		FixedResultPipe fixedResultPipe = new FixedResultPipe();
//		fixedResultPipe.setName("placeholder");
//		fixedResultPipe.setReturnString("placeholder");
//
//		pipeLine.addPipe(fixedResultPipe);
//		adapter.setPipeLine(pipeLine);
//
//		return adapter;
//	}

//	private void generateAPI(OpenAPI openAPI, File baseDir, String adapterFilenamePrefix, Message message, PipeLineSession session) throws PipeRunException, IOException {
//		String configurationName = openAPI.getInfo().getTitle() + " - " + openAPI.getInfo().getVersion();
//
//		List<Adapter> adapters = null;
//		try {
//			adapters = createConfiguration(openAPI);
//		}catch(Exception e) {
//			e.printStackTrace();
//			throw new PipeRunException(this, "failed to create configuration from openAPI ["+getOpenApiFilename()+"]",e);
//		}
//
//		File tempDir = new File(baseDir, configurationName + "\\");
//		if(!tempDir.exists()) tempDir.mkdirs();
//
//		Context ctx = null;
//		for(Adapter adapter : adapters) {
//			FileOutputStream fileOutputStream = null;
//			try {
////				ctx = buildContextFromParameters(message, session);
//				ctx = Context.newContext(adapter);
//				String resolved = template.apply(ctx);
//				File adapterFile = new File(tempDir, adapterFilenamePrefix + adapter.getName() + ".xml");
//				fileOutputStream = new FileOutputStream(adapterFile);
//				fileOutputStream.write(resolved.getBytes());
//			} catch (IOException e) {
//				e.printStackTrace();
//				throw new PipeRunException(this, "error building handlebars context from parameters", e);
//			} finally {
//				if(fileOutputStream != null) {
//					fileOutputStream.close();
//				}
//			}
//		}
//
//		Map<String, Object> jsonRoot = new HashMap<String, Object>();
//		List<Object> jsonModules = new ArrayList<Object>();
//		for(Adapter adapter : adapters) {
//			Map<String, Object> moduleObject = new HashMap<String, Object>();
//			moduleObject.put("name", adapter.getName());
//			moduleObject.put("path", "./" + adapterFilenamePrefix + adapter.getName() + ".xml");
//			jsonModules.add(moduleObject);
//		}
//		jsonRoot.put("name", configurationName);
//		jsonRoot.put("modules", jsonModules);
//
//		FileOutputStream fileOutputStream = null;
//		try {
//			ctx = Context.newContext(jsonRoot);
//			String resolved = configurationTemplate.apply(ctx);
//			File configurationFile = new File(tempDir, "Configuration.xml");
//			fileOutputStream = new FileOutputStream(configurationFile);
//			fileOutputStream.write(resolved.getBytes());
//		} catch (IOException e) {
//			e.printStackTrace();
//			throw new PipeRunException(this, "error building handlebars context from parameters", e);
//		} finally {
//			if(fileOutputStream != null) {
//				fileOutputStream.close();
//			}
//		}
//	}

//	private PipeRunResult generateAllApis(Message message, PipeLineSession session) throws PipeRunException {
//		OpenAPI zakenOpenAPI = null;
//		OpenAPI documentenOpenAPI = null;
//		OpenAPI catalogiOpenAPI = null;
//
//		try {
//			Resource oasFile = Resource.getResource(this, "zaken/openapi.yaml");
//			String yaml = new BufferedReader(
//					new InputStreamReader(oasFile.openStream(), StandardCharsets.UTF_8))
//						.lines()
//						.collect(Collectors.joining("\n"));
//			zakenOpenAPI = new OpenAPIV3Parser().readContents(yaml).getOpenAPI();
//			oasFile = Resource.getResource(this, "documenten/openapi.yaml");
//			yaml = new BufferedReader(
//					new InputStreamReader(oasFile.openStream(), StandardCharsets.UTF_8))
//						.lines()
//						.collect(Collectors.joining("\n"));
//			documentenOpenAPI = new OpenAPIV3Parser().readContents(yaml).getOpenAPI();
//			oasFile = Resource.getResource(this, "catalogi/openapi.yaml");
//			yaml = new BufferedReader(
//					new InputStreamReader(oasFile.openStream(), StandardCharsets.UTF_8))
//						.lines()
//						.collect(Collectors.joining("\n"));
//			catalogiOpenAPI = new OpenAPIV3Parser().readContents(yaml).getOpenAPI();
//		} catch (IOException e) {
//			throw new PipeRunException(this, "could not find file ["+getOpenApiFilename()+"]",e);
//		}
//
//		try {
//			File tempDir  = FileUtils.getTempDirectory("zgw-apis");
//			generateAPI(zakenOpenAPI, tempDir, "configuration_zaken_", message, session);
//			generateAPI(documentenOpenAPI, tempDir, "configuration_documenten_", message, session);
//			generateAPI(catalogiOpenAPI, tempDir, "configuration_catalogi_", message, session);
//		} catch (IOException e) {
//			e.printStackTrace();
//			return new PipeRunResult(getSuccessForward(), "<root/>");
//		}
//
//		return new PipeRunResult(getSuccessForward(), null);
//	}

	protected Context buildContextFromParameters(Message message, PipeLineSession session) throws PipeRunException, CollectionException {
		return Context.newBuilder(this.getParameterValueList(message, session))
				.resolver(ParameterListValueResolver.INSTANCE, DomdocValueResolver.INSTANCE)
				.build();
	}

	protected ParameterValueList getParameterValueList(Message input, PipeLineSession session) throws CollectionException {
		try {
			return getParameterList().getValues(input, session);
		} catch (ParameterException e) {
			throw new CollectionException("cannot determine parameter values", e);
		}
	}

	protected Context buildContextFromMessage(Message message, PipeLineSession session) throws PipeRunException {
		JsonNode jsonNode = null;
		try {
			jsonNode = new ObjectMapper().readValue(message.asInputStream(), JsonNode.class);
		} catch (IOException e) {

		}

		if(jsonNode != null) {
			return Context.newBuilder(jsonNode)
					.resolver(JsonNodeValueResolver.INSTANCE)
					.build();
		}

		Document domdoc = null;
		try {
			domdoc = XmlUtils.buildDomDocument(message.asString(), false);

		} catch (IOException | DomBuilderException e) {
			e.printStackTrace();
		}

		if(domdoc != null) {
			return Context.newBuilder(domdoc)
					.resolver(DomdocValueResolver.INSTANCE)
					.build();
		}

		return Context.newBuilder(message)
				.build();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		Context ctx = null;
		try {
			Context messageCtx = buildContextFromMessage(message, session);
			Context pvlCtx = buildContextFromParameters(message, session);

			Context.newBuilder(messageCtx).resolver(ParameterListValueResolver.INSTANCE, DomdocValueResolver.INSTANCE, JsonNodeValueResolver.INSTANCE);
			Context.newBuilder(pvlCtx).resolver(ParameterListValueResolver.INSTANCE, DomdocValueResolver.INSTANCE, JsonNodeValueResolver.INSTANCE);

			ctx = Context.newBuilder(message)
					.combine("payload", messageCtx.model())   // Access with {{doc.something}} if resolver supports it
					.combine("params", pvlCtx.model())    // Access with {{params.foo}}
					.resolver(
							ParameterListValueResolver.INSTANCE,
							DomdocValueResolver.INSTANCE,
							JsonNodeValueResolver.INSTANCE
					)
					.build();

//			Map<String, Object> merged = new HashMap<>();
//			merged.putAll((Map<String, Object>) messageCtx.model()); // base values
//			merged.putAll((Map<String, Object>) pvlCtx.model());     // params override if key conflict
//
//			ctx = Context.newBuilder(merged)
//					.resolver(
//							ParameterListValueResolver.INSTANCE,
//							DomdocValueResolver.INSTANCE,
//							JsonNodeValueResolver.INSTANCE
//					)
//					.build();

		} catch (PipeRunException | CollectionException e) {
			throw new PipeRunException(this, "error building handlebars context.", e);
		}

		String result = null;
		try {
			result = template.apply(ctx);
		} catch (IOException e) {
			throw new PipeRunException(this, "error rendering context onto template.", e);
		}

		return new PipeRunResult(getSuccessForward(), result);
	}

	/**
	 * Handlebars template file
	 */
	public void setTemplateFile(String templateFile) {
		this.templateFile = templateFile;
	}

	/**
	 * Handlebars template name
	 */
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	/**
	 * Handlebars template name sessionKey
	 */
	public void setTemplateNameSessionKey(String templateNameSessionKey) {
		this.templateNameSessionKey = templateNameSessionKey;
	}

	/**
	 * @param useInputAsContext the useInputAsContext to set
	 */
	public void setUseInputAsContext(boolean useInputAsContext) {
		this.useInputAsContext = useInputAsContext;
	}

	/**
	 * @param contextSessionKey the contextSessionKey to set
	 */
	public void setContextSessionKey(String contextSessionKey) {
		this.contextSessionKey = contextSessionKey;
	}

	/**
	 * @param contextParamName the contextParamName to set
	 */
	public void setContextParamName(String contextParamName) {
		this.contextParamName = contextParamName;
	}

	/**
	 * OpenApi schema filename.
	 */
	public void setOpenApiFilename(String filename) {
		this.openApiFilename = filename;
	}
}

