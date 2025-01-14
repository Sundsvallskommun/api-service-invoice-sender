package se.sundsvall.invoicesender.configuration;

import com.google.common.jimfs.Jimfs;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class LocalFileSystemConfiguration {

	@Bean
	FileSystem defaultLocalFileSystem(@Value("${local-file-system.in-memory:false}") final boolean useInMemoryFileSystem) {
		if (useInMemoryFileSystem) {
			return Jimfs.newFileSystem(com.google.common.jimfs.Configuration.unix());
		}
		return FileSystems.getDefault();
	}
}
