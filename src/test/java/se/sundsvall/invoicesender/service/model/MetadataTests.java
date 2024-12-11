package se.sundsvall.invoicesender.service.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEquals;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCode;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

class MetadataTests {

	@Test
	void testBean() {
		MatcherAssert.assertThat(Metadata.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCode(),
			hasValidBeanEquals(),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderPattern() {
		var metadata = new Metadata()
			.withInvoiceNumber("123")
			.withInvoiceDate("2021-01-01")
			.withDueDate("2021-01-31")
			.withAccountNumber("1234567890")
			.withPaymentReference("1234567890")
			.withTotalAmount("123.45")
			.withPayable(true)
			.withReminder(false);

		assertThat(metadata.getInvoiceNumber()).isEqualTo("123");
		assertThat(metadata.getInvoiceDate()).isEqualTo("2021-01-01");
		assertThat(metadata.getDueDate()).isEqualTo("2021-01-31");
		assertThat(metadata.getAccountNumber()).isEqualTo("1234567890");
		assertThat(metadata.getPaymentReference()).isEqualTo("1234567890");
		assertThat(metadata.getTotalAmount()).isEqualTo("123.45");
		assertThat(metadata.isPayable()).isTrue();
		assertThat(metadata.isReminder()).isFalse();
	}

	@Test
	void testSettersAndGetters() {
		var metadata = new Metadata();
		metadata.setInvoiceNumber("123");
		metadata.setInvoiceDate("2021-01-01");
		metadata.setDueDate("2021-01-31");
		metadata.setAccountNumber("1234567890");
		metadata.setPaymentReference("1234567890");
		metadata.setTotalAmount("123.45");
		metadata.setPayable(true);
		metadata.setReminder(false);

		assertThat(metadata.getInvoiceNumber()).isEqualTo("123");
		assertThat(metadata.getInvoiceDate()).isEqualTo("2021-01-01");
		assertThat(metadata.getDueDate()).isEqualTo("2021-01-31");
		assertThat(metadata.getAccountNumber()).isEqualTo("1234567890");
		assertThat(metadata.getPaymentReference()).isEqualTo("1234567890");
		assertThat(metadata.getTotalAmount()).isEqualTo("123.45");
		assertThat(metadata.isPayable()).isTrue();
		assertThat(metadata.isReminder()).isFalse();
	}
}
