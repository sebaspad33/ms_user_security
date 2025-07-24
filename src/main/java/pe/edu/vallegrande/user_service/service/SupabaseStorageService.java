package pe.edu.vallegrande.user_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageService {

    @Value("${supabase.project-url}")
    private String supabaseUrl;

    @Value("${supabase.api-key}")
    private String supabaseApiKey;

    @Value("${supabase.bucket}")
    private String supabaseBucket;

    @Value("${supabase.folder}")
    private String supabaseFolder;

    /**
     * Sube una imagen al bucket Supabase y devuelve la URL pública
     */
    public Mono<String> uploadImage(FilePart filePart) {
        String fileName = UUID.randomUUID() + "_" + filePart.filename();
        String filePath = supabaseFolder + "/" + fileName;

        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    return WebClient.builder()
                            .baseUrl(supabaseUrl + "/storage/v1/object")
                            .defaultHeader("Authorization", "Bearer " + supabaseApiKey)
                            .defaultHeader("Content-Type", filePart.headers().getContentType().toString())
                            .build()
                            .put()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/" + supabaseBucket + "/" + filePath)
                                    .queryParam("upsert", true)
                                    .build())
                            .body(BodyInserters.fromValue(bytes))
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(resp -> supabaseUrl + "/storage/v1/object/public/" + supabaseBucket + "/" + filePath);
                });
    }

    /**
     * Elimina una imagen de Supabase Storage en base a su URL pública
     */
    public Mono<Boolean> deleteImage(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) return Mono.just(false);

        // Extrae el path del archivo desde la URL pública
        String filePrefix = supabaseUrl + "/storage/v1/object/public/" + supabaseBucket + "/";
        if (!publicUrl.startsWith(filePrefix)) {
            log.warn("URL no pertenece a Supabase configurado: {}", publicUrl);
            return Mono.just(false);
        }

        String filePath = publicUrl.replace(filePrefix, "");

        return WebClient.builder()
                .baseUrl(supabaseUrl + "/storage/v1/object")
                .defaultHeader("Authorization", "Bearer " + supabaseApiKey)
                .build()
                .delete()
                .uri("/" + supabaseBucket + "/" + filePath)
                .retrieve()
                .bodyToMono(String.class)
                .map(resp -> true)
                .onErrorResume(err -> {
                    log.error("Error al eliminar imagen: {}", err.getMessage());
                    return Mono.just(false);
                });
    }
}
