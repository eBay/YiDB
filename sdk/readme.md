**How To use YiDB SDK as jar dependency**

<b>Step0<b> Build & install client library

<pre><code>
// clone code
cd sdk/
mvn clean install -Dmaven.test.skip
</code></pre>

Now the sdk library is installed into your local repository. You can now reference it as normal maven dependency.

If you don't want to use the code generation, you can directly use the library install above by have this dependency in your projects' pom.xml.

<pre>
<code>
	&lt;dependencies&gt;
		&lt;dependency&gt;
			&lt;groupId&gt;com.ebay.cloud.cms.typsafe&lt;/groupId&gt;
			&lt;artifactId&gt;cms-java-typesafe-api&lt;/artifactId&gt;
			&lt;version&gt;1.0.26-SNAPSHOT&lt;/version&gt;
		&lt;/dependency&gt;
	&lt;/dependencies&gt;
</code>
</pre>

If you want to used typesafed code generation, continue to follow below.

<b>Step1</b>  Use code generator to generate the entity model of the YiDB repo

It's suggestted to use maven to resolve the dependency for cms-java-model-exporter-0.0.1-SNAPSHOT.jar to generate the models.

NOTE: The version below might be changed according to the code you pulled.

A well structured model and code generatation project structure is like below:
<pre>
<code>
model-parent
	--  model-def
	--  model
</code>
</pre>

*Model-parent*

Assume user metadata json is placed unser metadata/package1.

<pre>
&lt;project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"&gt;
  &lt;modelVersion&gt;4.0.0&lt;/modelVersion&gt;
  &lt;groupId&gt;com.cloud.cms&lt;/groupId&gt;
  &lt;artifactId&gt;model-parent&lt;/artifactId&gt;
  &lt;version&gt;0.0.1-SNAPSHOT&lt;/version&gt;
  &lt;packaging&gt;pom&lt;/packaging&gt;
  &lt;modules&gt;
  	&lt;module&gt;model&lt;/module&gt;
  	&lt;module&gt;model-def&lt;/module&gt;
  &lt;/modules&gt;
&lt;/project&gt;
</pre>

*Mode-def*

<pre>
<code>
&lt;project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"&gt;
	&lt;modelVersion&gt;4.0.0&lt;/modelVersion&gt;

	&lt;groupId&gt;com.cloud.cms&lt;/groupId&gt;
	&lt;artifactId&gt;model-def&lt;/artifactId&gt;
	&lt;version&gt;0.0.1-SNAPSHOT&lt;/version&gt;
	&lt;packaging&gt;jar&lt;/packaging&gt;

	&lt;name&gt;model-def&lt;/name&gt;

	&lt;properties&gt;
		&lt;project.build.sourceEncoding&gt;UTF-8&lt;/project.build.sourceEncoding&gt;
	&lt;/properties&gt;
	&lt;build&gt;
		&lt;plugins&gt;
			&lt;plugin&gt;
				&lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
				&lt;artifactId&gt;maven-resources-plugin&lt;/artifactId&gt;
				&lt;version&gt;2.4&lt;/version&gt;
				&lt;configuration&gt;
					&lt;encoding&gt;UTF-8&lt;/encoding&gt;
				&lt;/configuration&gt;
			&lt;/plugin&gt;
		&lt;/plugins&gt;

		&lt;resources&gt;
			&lt;resource&gt;
				&lt;directory&gt;metadata/package1&lt;/directory&gt;
				&lt;targetPath&gt;com/cloud/cms/model/package1&lt;/targetPath&gt;
				&lt;includes&gt;
					&lt;include&gt;*.json&lt;/include&gt;
				&lt;/includes&gt;
			&lt;/resource&gt;
		&lt;/resources&gt;

	&lt;/build&gt;
&lt;/project&gt;
</code>
</pre>

*Model*

<pre>
<code>
&lt;project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd"&gt;
	&lt;modelVersion&gt;4.0.0&lt;/modelVersion&gt;

	&lt;groupId&gt;com.cloud.cms&lt;/groupId&gt;
	&lt;artifactId&gt;model&lt;/artifactId&gt;
	&lt;version&gt;0.0.1-SNAPSHOT&lt;/version&gt;
	&lt;packaging&gt;jar&lt;/packaging&gt;

	&lt;name&gt;model&lt;/name&gt;

	&lt;properties&gt;
		&lt;project.build.sourceEncoding&gt;UTF-8&lt;/project.build.sourceEncoding&gt;
	&lt;/properties&gt;

	&lt;dependencies&gt;
		&lt;dependency&gt;
			&lt;groupId&gt;junit&lt;/groupId&gt;
			&lt;artifactId&gt;junit&lt;/artifactId&gt;
			&lt;version&gt;3.8.1&lt;/version&gt;
			&lt;scope&gt;test&lt;/scope&gt;
		&lt;/dependency&gt;
		&lt;dependency&gt;
			&lt;groupId&gt;com.cloud.cms&lt;/groupId&gt;
			&lt;artifactId&gt;model-def&lt;/artifactId&gt;
			&lt;version&gt;0.0.1-SNAPSHOT&lt;/version&gt;
		&lt;/dependency&gt;
		&lt;dependency&gt;
			&lt;groupId&gt;com.ebay.cloud.cms.typsafe&lt;/groupId&gt;
			&lt;artifactId&gt;cms-java-typesafe-api&lt;/artifactId&gt;
			&lt;version&gt;1.0.26-SNAPSHOT&lt;/version&gt;
		&lt;/dependency&gt;
		&lt;dependency&gt;
			&lt;groupId&gt;com.ebay.cloud.cms.typsafe&lt;/groupId&gt;
			&lt;artifactId&gt;cms-java-model-exporter&lt;/artifactId&gt;
			&lt;version&gt;1.0.26-SNAPSHOT&lt;/version&gt;
		&lt;/dependency&gt;
	&lt;/dependencies&gt;

	&lt;build&gt;
		&lt;plugins&gt;
			&lt;plugin&gt;
				&lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
				&lt;artifactId&gt;maven-compiler-plugin&lt;/artifactId&gt;
				&lt;configuration&gt;
					&lt;source&gt;1.6&lt;/source&gt;
					&lt;target&gt;1.6&lt;/target&gt;
				&lt;/configuration&gt;
			&lt;/plugin&gt;
			&lt;plugin&gt;
				&lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
				&lt;artifactId&gt;maven-eclipse-plugin&lt;/artifactId&gt;
				&lt;version&gt;2.9&lt;/version&gt;
				&lt;configuration&gt;
					&lt;sourceIncludes&gt;
						&lt;sourceInclude&gt;target/generated/**&lt;/sourceInclude&gt;
					&lt;/sourceIncludes&gt;
				&lt;/configuration&gt;
			&lt;/plugin&gt;
			&lt;plugin&gt;
				&lt;groupId&gt;org.codehaus.mojo&lt;/groupId&gt;
				&lt;artifactId&gt;exec-maven-plugin&lt;/artifactId&gt;
				&lt;version&gt;1.2.1&lt;/version&gt;
				&lt;executions&gt;
					&lt;execution&gt;
						&lt;id&gt;generate-sources-cms&lt;/id&gt;
						&lt;phase&gt;generate-sources&lt;/phase&gt;
						&lt;goals&gt;
							&lt;goal&gt;java&lt;/goal&gt;
						&lt;/goals&gt;
						&lt;configuration&gt;
							&lt;sourceRoot&gt;${project.build.directory}/generated/cms&lt;/sourceRoot&gt;
							&lt;mainClass&gt;com.ebay.cloud.cms.typsafe.exporter.CodeGenerationMain&lt;/mainClass&gt;
							&lt;arguments&gt;
								&lt;argument&gt;com.cloud.cms.model.package1&lt;/argument&gt;
								&lt;argument&gt;${project.build.directory}/generated/cms/&lt;/argument&gt;
								&lt;argument&gt;com.cloud.cms.model.package1&lt;/argument&gt;
							&lt;/arguments&gt;
							&lt;includePluginDependencies&gt;true&lt;/includePluginDependencies&gt;
							&lt;executableDependency&gt;
								&lt;groupId&gt;com.ebay.cloud.cms.typsafe&lt;/groupId&gt;
								&lt;artifactId&gt;cms-java-model-exporter&lt;/artifactId&gt;
							&lt;/executableDependency&gt;
						&lt;/configuration&gt;
					&lt;/execution&gt;
				&lt;/executions&gt;
				&lt;dependencies&gt;
					&lt;dependency&gt;
						&lt;groupId&gt;com.ebay.cloud.cms.typsafe&lt;/groupId&gt;
						&lt;artifactId&gt;cms-java-model-exporter&lt;/artifactId&gt;
						&lt;version&gt;1.0.26-SNAPSHOT&lt;/version&gt;
					&lt;/dependency&gt;
					&lt;dependency&gt;
						&lt;groupId&gt;com.cloud.cms&lt;/groupId&gt;
						&lt;artifactId&gt;model-def&lt;/artifactId&gt;
						&lt;version&gt;0.0.1-SNAPSHOT&lt;/version&gt;
					&lt;/dependency&gt;
				&lt;/dependencies&gt;
			&lt;/plugin&gt;
			&lt;plugin&gt;
				&lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
				&lt;artifactId&gt;maven-source-plugin&lt;/artifactId&gt;
				&lt;version&gt;2.2.1&lt;/version&gt;
				&lt;executions&gt;
					&lt;execution&gt;
						&lt;id&gt;attach-sources&lt;/id&gt;
						&lt;phase&gt;verify&lt;/phase&gt;
						&lt;goals&gt;
							&lt;goal&gt;jar-no-fork&lt;/goal&gt;
						&lt;/goals&gt;
					&lt;/execution&gt;
				&lt;/executions&gt;
			&lt;/plugin&gt;
		&lt;/plugins&gt;
	&lt;/build&gt;
&lt;/project&gt;

</code>
</pre>





