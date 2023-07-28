package usi.si.seart.git;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import usi.si.seart.exception.TerminalExecutionException;
import usi.si.seart.exception.git.GitException;
import usi.si.seart.io.TerminalExecution;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Component responsible for cloning git repositories into a temporary folder.
 */
@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class GitRepositoryCloner {

    @NonFinal
    @Value("${app.crawl.analysis.folder-prefix}")
    String folderPrefix;

    ConversionService conversionService;

    /**
     * Clones a git repository into a temporary folder
     * and returns a handle for the cloned repository.
     *
     * @param url the URL corresponding to the git repository.
     * @return the handle of the cloned repository.
     */
    @SuppressWarnings("ConstantConditions")
    public Future<LocalRepositoryClone> clone(URL url) {
        try {
            Path directory = Files.createTempDirectory(folderPrefix);
            TerminalExecution execution = new TerminalExecution(
                    directory, "git", "clone", "--quiet", "--depth", "1", url.toString(), directory.toString()
            );
            log.trace(" Cloning repository: {}", url);
            TerminalExecution.Result result = execution.execute(5, TimeUnit.MINUTES);
            if (result.succeeded()) {
                return CompletableFuture.completedFuture(new LocalRepositoryClone(directory));
            } else {
                GitException exception = conversionService.convert(result.getStdErr(), GitException.class);
                return CompletableFuture.failedFuture(exception);
            }
        } catch (IOException | TerminalExecutionException | TimeoutException ex) {
            return CompletableFuture.failedFuture(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(ex);
        }
    }
}
