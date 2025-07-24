package pe.edu.vallegrande.user_service.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord.CreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.edu.vallegrande.user_service.dto.UserCreateDto;
import pe.edu.vallegrande.user_service.dto.UserDto;
import pe.edu.vallegrande.user_service.model.User;
import pe.edu.vallegrande.user_service.repository.UsersRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class UserService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final SupabaseStorageService supabaseStorageService;

    @Autowired
    public UserService(UsersRepository usersRepository, PasswordEncoder passwordEncoder, SupabaseStorageService supabaseStorageService) {
        this.usersRepository = usersRepository;
        this.passwordEncoder = passwordEncoder;
        this.supabaseStorageService = supabaseStorageService;
    }

    /**
     * Crear un nuevo usuario: registra en Firebase y luego en la base de datos.
     */
    public Mono<UserDto> createUser(UserCreateDto dto, FilePart image) {
        return emailExists(dto.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("❌ El correo ya está registrado."));
                    }

                    CreateRequest request = new CreateRequest()
                            .setEmail(dto.getEmail())
                            .setPassword(dto.getPassword())
                            .setEmailVerified(false)
                            .setDisabled(false);

                    return Mono.fromCallable(() -> FirebaseAuth.getInstance().createUser(request))
                            .flatMap(firebaseUser -> {
                                String uid = firebaseUser.getUid();
                                String primaryRole = dto.getRole().isEmpty() ? "USER" : dto.getRole().get(0);
                                return Mono.fromCallable(() -> {
                                    FirebaseAuth.getInstance().setCustomUserClaims(uid, Map.of("role", primaryRole.toUpperCase()));
                                    return uid;
                                });
                            })
                            .flatMap(uid -> {
                                Mono<String> uploadMono = image != null ?
                                        supabaseStorageService.uploadImage(image) : Mono.just("");

                                return uploadMono.flatMap(imageUrl -> {
                                    User user = new User();
                                    user.setFirebaseUid(uid);
                                    user.setName(dto.getName());
                                    user.setLastName(dto.getLastName());
                                    user.setDocumentType(dto.getDocumentType());
                                    user.setDocumentNumber(dto.getDocumentNumber());
                                    user.setCellPhone(dto.getCellPhone());
                                    user.setEmail(dto.getEmail());
                                    user.setPassword(passwordEncoder.encode(dto.getPassword()));
                                    user.setRole(dto.getRole());
                                    user.setProfileImage(imageUrl);
                                    return usersRepository.save(user).map(this::toDto);
                                });
                            });
                });
    }

    /**
     * Actualiza datos de un usuario por ID, sin modificar email ni contraseña.
     */
    public Mono<UserDto> updateUser(Integer id, UserDto dto, FilePart newImage) {
        return usersRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuario no encontrado")))
                .flatMap(existing -> {
                    existing.setName(dto.getName());
                    existing.setLastName(dto.getLastName());
                    existing.setDocumentType(dto.getDocumentType());
                    existing.setDocumentNumber(dto.getDocumentNumber());
                    existing.setCellPhone(dto.getCellPhone());
                    existing.setRole(dto.getRole());

                    Mono<String> imageMono = Mono.just(existing.getProfileImage());

                    if (newImage != null) {
                        imageMono = supabaseStorageService.deleteImage(existing.getProfileImage())
                                .then(supabaseStorageService.uploadImage(newImage));
                    }

                    return imageMono.flatMap(imageUrl -> {
                        existing.setProfileImage(imageUrl);
                        return usersRepository.save(existing).map(this::toDto);
                    });
                });
    }

    /**
     * Devuelve los datos del usuario actual por su UID de Firebase.
     */
    public Mono<UserDto> findMyProfile(String firebaseUid) {
        return usersRepository.findAll()
                .filter(user -> firebaseUid.equals(user.getFirebaseUid()))
                .next()
                .map(this::toDto)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")));
    }

    /**
     * Devuelve todos los usuarios registrados.
     */
    public Flux<UserDto> findAllUsers() {
        return usersRepository.findAll().map(this::toDto);
    }

    /**
     * Devuelve un usuario por su ID.
     */
    public Mono<UserDto> findById(Integer id) {
        return usersRepository.findById(id).map(this::toDto);
    }

    /**
     * Devuelve un usuario por su email.
     */
    public Mono<UserDto> findByEmail(String email) {
        return usersRepository.findByEmail(email).map(this::toDto);
    }

    /**
     * Verifica si un email ya está registrado.
     */
    public Mono<Boolean> emailExists(String email) {
        return findByEmail(email).hasElement();
    }

    /**
     * Elimina un usuario por ID de Firebase y la base de datos.
     */
    public Mono<Void> deleteUser(Integer id) {
        return usersRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                .flatMap(user -> {
                    String firebaseUid = user.getFirebaseUid();
                    Mono<Void> firebaseDeletion = Mono.fromCallable(() -> {
                        FirebaseAuth.getInstance().deleteUser(firebaseUid);
                        return null;
                    });
                    Mono<Void> dbDeletion = usersRepository.deleteById(user.getId());
                    return firebaseDeletion.then(dbDeletion);
                });
    }

    /**
     * Cambia el email del usuario en Firebase y en la base de datos.
     */
    public Mono<UserDto> changeEmail(String firebaseUid, String newEmail) {
        return usersRepository.findAll()
                .filter(user -> firebaseUid.equals(user.getFirebaseUid()))
                .next()
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                .flatMap(user -> usersRepository.findByEmail(newEmail)
                        .flatMap(conflict -> Mono.<User>error(new RuntimeException("El correo ya está en uso")))
                        .switchIfEmpty(Mono.defer(() -> {
                            return Mono.fromCallable(() -> {
                                FirebaseAuth.getInstance().updateUser(
                                        new com.google.firebase.auth.UserRecord.UpdateRequest(firebaseUid)
                                                .setEmail(newEmail)
                                );
                                return user;
                            });
                        })))
                .flatMap(updatedUser -> {
                    updatedUser.setEmail(newEmail);
                    return usersRepository.save(updatedUser).map(this::toDto);
                });
    }

    /**
     * Cambia la contraseña en Firebase y la actualiza en la base de datos.
     */
    public Mono<UserDto> changePassword(String firebaseUid, String newPassword) {
        return usersRepository.findAll()
                .filter(user -> firebaseUid.equals(user.getFirebaseUid()))
                .next()
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                .flatMap(user -> Mono.fromCallable(() -> {
                    FirebaseAuth.getInstance().updateUser(
                            new com.google.firebase.auth.UserRecord.UpdateRequest(firebaseUid)
                                    .setPassword(newPassword)
                    );
                    return user;
                }))
                .flatMap(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    return usersRepository.save(user).map(this::toDto);
                });
    }

    /**
     * Permite al usuario editar su propio perfil sin cambiar email, password ni rol.
     */
    public Mono<UserDto> updateMyProfile(String uid, UserDto updatedData) {
        return usersRepository.findByFirebaseUid(uid)
                .switchIfEmpty(Mono.error(new RuntimeException("Usuario no encontrado")))
                .flatMap(existing -> {
                    existing.setName(updatedData.getName());
                    existing.setLastName(updatedData.getLastName());
                    existing.setDocumentType(updatedData.getDocumentType());
                    existing.setDocumentNumber(updatedData.getDocumentNumber());
                    existing.setCellPhone(updatedData.getCellPhone());
                    existing.setProfileImage(updatedData.getProfileImage());
                    return usersRepository.save(existing);
                })
                .map(UserDto::fromEntity);
    }

    /**
     * Convierte la entidad User a UserDto
     */
    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getFirebaseUid(),
                user.getName(),
                user.getLastName(),
                user.getDocumentType(),
                user.getDocumentNumber(),
                user.getCellPhone(),
                user.getEmail(),
                user.getRole(),
                user.getProfileImage()
        );
    }
}
