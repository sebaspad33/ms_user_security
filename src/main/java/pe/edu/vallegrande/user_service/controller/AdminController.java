package pe.edu.vallegrande.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import pe.edu.vallegrande.user_service.dto.UserCreateDto;
import pe.edu.vallegrande.user_service.dto.UserDto;
import pe.edu.vallegrande.user_service.service.UserService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/admin/users")
public class AdminController {

    private final UserService userService;

    @Autowired
    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // 🔍 Obtener todos los usuarios
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<UserDto> getAllUsers() {
        return userService.findAllUsers();
    }

    // 🔍 Obtener usuario por ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<UserDto> getUserById(@PathVariable Integer id) {
        return userService.findById(id);
    }

    // 🔍 Obtener usuario por Email
    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<UserDto> getUserByEmail(@PathVariable String email) {
        return userService.findByEmail(email);
    }

    // ✅ Verificar si un email ya está registrado
    @GetMapping("/email-exists/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Boolean>> checkIfEmailExists(@PathVariable String email) {
        return userService.emailExists(email)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(false)));
    }

    // 🆕 Crear usuario con imagen
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<UserDto> createUser(@RequestPart("user") String userJson,
                                    @RequestPart(value = "file", required = false) FilePart file) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            UserCreateDto dto = mapper.readValue(userJson, UserCreateDto.class);
            return userService.createUser(dto, file);
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("Error al parsear JSON: " + e.getMessage()));
        }
    }

    // ✏️ Actualizar usuario con nueva imagen (si aplica)
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<UserDto> updateUser(@PathVariable Integer id,
                                    @RequestPart("user") String userJson,
                                    @RequestPart(value = "file", required = false) FilePart file) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            UserDto dto = mapper.readValue(userJson, UserDto.class);
            return userService.updateUser(id, dto, file);
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("Error al parsear JSON: " + e.getMessage()));
        }
    }


    // 🗑️ Eliminar usuario
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Integer id) {
        return userService.deleteUser(id)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().<Void>build()));
    }
}
