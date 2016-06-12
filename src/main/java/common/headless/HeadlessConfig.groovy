package common.headless

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource

@Configuration
@ComponentScan('com.bc.test')
@Profile('headless')
class HeadlessConfig {
	@Bean
	yamlProperties() {
		new YamlPropertiesFactoryBean(
			resources: [new ClassPathResource('headless.yml')]
		)
	}

	@Bean
	PropertyPlaceholderConfigurer placeHolderConfigurer() {
		new PropertyPlaceholderConfigurer(
			properties: yamlProperties()
		)
	}
}
