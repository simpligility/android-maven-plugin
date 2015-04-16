package com.simpligility.maven.plugins.android.config;

import com.simpligility.maven.plugins.android.config.ConfigPojo;
import com.simpligility.maven.plugins.android.config.PullParameter;

public class DummyMojo {

	@ConfigPojo
	private DummyConfigPojo configPojo;

	// Maven injected parameters
	private String configPojoStringValue;
	private Boolean configPojoBooleanValue;
    private String[] configPojoMethodValue;

	@PullParameter(defaultValue = "hello")
	private String parsedStringValue;

	@PullParameter(defaultValue = "true")
	private Boolean parsedBooleanValue;

	@PullParameter(defaultValueGetterMethod = "getDefaultMethodValue")
	private String[] parsedMethodValue;

	public String[] getDefaultMethodValue()
	{
		return new String[] {"a","b"};
	}
	
	public void setConfigPojo(DummyConfigPojo configPojo) {
		this.configPojo = configPojo;
	}

	public void setConfigPojoStringValue(String configPojoStringValue) {
		this.configPojoStringValue = configPojoStringValue;
	}

	public void setConfigPojoBooleanValue(Boolean configPojoBooleanValue) {
		this.configPojoBooleanValue = configPojoBooleanValue;
	}

	public DummyConfigPojo getConfigPojo() {
		return configPojo;
	}

	public String getConfigPojoStringValue() {
		return configPojoStringValue;
	}

	public Boolean getConfigPojoBooleanValue() {
		return configPojoBooleanValue;
	}

	public String getParsedStringValue() {
		return parsedStringValue;
	}

	public Boolean getParsedBooleanValue() {
		return parsedBooleanValue;
	}

	public String[] getParsedMethodValue() {
		return parsedMethodValue;
	}

}
