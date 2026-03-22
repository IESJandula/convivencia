package com.iesjandula.convivencia.service;

import com.iesjandula.convivencia.entity.Grupo;
import com.iesjandula.convivencia.entity.ParteDisciplinario;
import com.iesjandula.convivencia.entity.Profesor;
import com.iesjandula.convivencia.repository.AlumnoRepository;
import com.iesjandula.convivencia.repository.GrupoRepository;
import com.iesjandula.convivencia.repository.ParteDisciplinarioRepository;
import com.iesjandula.convivencia.repository.ProfesorRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.text.Normalizer;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MonitorizacionService {

    private final GrupoRepository grupoRepository;
    private final ParteDisciplinarioRepository parteRepository;
    private final ProfesorRepository profesorRepository;
    private final AlumnoRepository alumnoRepository;

    public MonitorizacionService(GrupoRepository grupoRepository,
                                 ParteDisciplinarioRepository parteRepository,
                                 ProfesorRepository profesorRepository,
                                 AlumnoRepository alumnoRepository) {
        this.grupoRepository = grupoRepository;
        this.parteRepository = parteRepository;
        this.profesorRepository = profesorRepository;
        this.alumnoRepository = alumnoRepository;
    }

    @Transactional
    public Optional<Grupo> obtenerGrupoTutoria(String tutorEmail) {
        if (tutorEmail == null || tutorEmail.trim().isEmpty()) {
            return Optional.empty();
        }

        String emailLimpio = tutorEmail.trim().toLowerCase(Locale.ROOT);
        Profesor profesor = profesorRepository.findByEmailNormalized(emailLimpio)
                .map(this::activarSiProcede)
                .orElseGet(() -> crearProfesorBasico(emailLimpio));

        Optional<Grupo> existente = grupoRepository.findByTutorEmailAndActivoTrue(profesor.getEmail());
        if (existente.isPresent()) {
            Grupo grupoExistente = existente.get();
            repararGrupoSiEsFicticio(grupoExistente);
            if (existeOtroTutorEnMismoCurso(grupoExistente, profesor.getEmail())) {
                grupoExistente.setTutor(null);
                grupoRepository.save(grupoExistente);
            } else {
                return Optional.of(grupoExistente);
            }
        }

        Grupo grupoAsignado = buscarGrupoLibreSinTutorDuplicado()
            .orElseGet(this::crearGrupoAleatorioDesdeAlumnado);

        repararGrupoSiEsFicticio(grupoAsignado);

        grupoRepository.findByTutorEmail(profesor.getEmail())
            .filter(grupo -> grupoAsignado.getId() == null || !grupo.getId().equals(grupoAsignado.getId()))
            .ifPresent(grupoAnterior -> {
                grupoAnterior.setTutor(null);
                grupoRepository.save(grupoAnterior);
            });

        grupoAsignado.setTutor(profesor);
        grupoAsignado.setActivo(true);
        return Optional.of(grupoRepository.save(grupoAsignado));
    }

    private Profesor activarSiProcede(Profesor profesor) {
        if (Boolean.FALSE.equals(profesor.getActivo())) {
            profesor.setActivo(true);
            return profesorRepository.save(profesor);
        }
        return profesor;
    }

    private Profesor crearProfesorBasico(String email) {
        Profesor profesor = new Profesor();
        profesor.setEmail(email);
        profesor.setNombre(nombreDesdeEmail(email));
        profesor.setRol(Profesor.Rol.PROFESOR);
        profesor.setEsGuardia(false);
        profesor.setActivo(true);
        return profesorRepository.save(profesor);
    }

    private String nombreDesdeEmail(String email) {
        String localPart = email;
        int at = email.indexOf('@');
        if (at > 0) {
            localPart = email.substring(0, at);
        }

        String base = localPart.replace('.', ' ').replace('_', ' ').trim();
        if (base.isBlank()) {
            return "Profesor";
        }
        return Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }

    public List<ParteDisciplinario> obtenerPartesTutoria(String tutorEmail) {
        Optional<Grupo> grupoOpt = obtenerGrupoTutoria(tutorEmail);
        if (grupoOpt.isEmpty()) {
            return Collections.emptyList();
        }

        Grupo grupo = grupoOpt.get();
        String cursoTutoria = normalizar(grupo.getCurso());
        String grupoTutoria = normalizar(grupo.getLetra());

        return parteRepository.findByActivoTrue().stream()
                .filter(parte -> parte.getAlumno() != null)
                .filter(parte -> normalizar(parte.getAlumno().getCurso()).equals(cursoTutoria))
                .filter(parte -> normalizar(parte.getAlumno().getGrupo()).equals(grupoTutoria))
                .sorted(Comparator.comparing(ParteDisciplinario::getFecha).reversed())
                .collect(Collectors.toList());
    }

    public List<ParteDisciplinario> obtenerPartesJefatura(String nombreAlumno, String curso, String grupo) {
        String nombreNormalizado = normalizarTexto(limpiar(nombreAlumno));
        String cursoNormalizado = normalizarTexto(limpiar(curso));
        String grupoNormalizado = normalizarTexto(limpiar(grupo));

        return parteRepository.findByActivoTrue().stream()
                .filter(parte -> parte.getAlumno() != null)
                .filter(parte -> {
                    if (nombreNormalizado == null) {
                        return true;
                    }
                    String nombreCompleto = (parte.getAlumno().getNombre() + " " + parte.getAlumno().getApellidos()).trim();
                    return normalizarTexto(nombreCompleto).contains(nombreNormalizado);
                })
                .filter(parte -> cursoNormalizado == null || normalizarTexto(parte.getAlumno().getCurso()).equals(cursoNormalizado))
                .filter(parte -> grupoNormalizado == null || normalizarTexto(parte.getAlumno().getGrupo()).equals(grupoNormalizado))
                .sorted(Comparator.comparing(ParteDisciplinario::getFecha).reversed())
                .collect(Collectors.toList());
    }

    public List<String> obtenerCursosJefatura() {
        return parteRepository.findByActivoTrue().stream()
                .filter(parte -> parte.getAlumno() != null && parte.getAlumno().getCurso() != null)
                .map(parte -> parte.getAlumno().getCurso().trim())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<String> obtenerGruposJefatura() {
        return parteRepository.findByActivoTrue().stream()
                .filter(parte -> parte.getAlumno() != null && parte.getAlumno().getGrupo() != null)
                .map(parte -> parte.getAlumno().getGrupo().trim())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private Grupo crearGrupoAleatorioDesdeAlumnado() {
        List<String> opciones = obtenerCombinacionesReales().stream()
            .filter(this::cursoSinTutorAsignado)
            .collect(Collectors.toList());
        Collections.shuffle(opciones);

        Grupo nuevo = new Grupo();
        if (!opciones.isEmpty()) {
            String[] partes = opciones.get(0).split("\\|", 2);
            nuevo.setCurso(partes[0]);
            nuevo.setLetra(partes[1]);
        } else {
            return grupoRepository.findByActivoTrue().stream()
                    .filter(g -> !esCursoFicticio(g.getCurso()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No hay cursos reales disponibles en alumnado para asignar tutoría"));
        }

        nuevo.setActivo(true);
        return grupoRepository.save(nuevo);
    }

    private List<String> obtenerCombinacionesReales() {
        Set<String> combinaciones = new LinkedHashSet<>();

        alumnoRepository.findByActivoTrue().forEach(alumno -> {
            String curso = alumno.getCurso() == null ? null : alumno.getCurso().trim();
            String grupo = alumno.getGrupo() == null ? null : alumno.getGrupo().trim().toUpperCase(Locale.ROOT);

            if (curso == null || curso.isBlank() || grupo == null || grupo.isBlank()) {
                return;
            }
            combinaciones.add(curso + "|" + grupo);
        });

        return new ArrayList<>(combinaciones);
    }

    private void repararGrupoSiEsFicticio(Grupo grupo) {
        if (grupo == null || !esCursoFicticio(grupo.getCurso())) {
            return;
        }

        List<String> opciones = obtenerCombinacionesReales().stream()
                .filter(this::cursoSinTutorAsignado)
                .collect(Collectors.toList());
        if (opciones.isEmpty()) {
            return;
        }

        Collections.shuffle(opciones);
        String[] partes = opciones.get(0).split("\\|", 2);
        grupo.setCurso(partes[0]);
        grupo.setLetra(partes[1]);
        grupoRepository.save(grupo);
    }

    private boolean esCursoFicticio(String curso) {
        if (curso == null) {
            return false;
        }

        String valor = curso.trim().toLowerCase(Locale.ROOT);
        return valor.startsWith("tutoria") || valor.startsWith("tutoría");
    }

    private Optional<Grupo> buscarGrupoLibreSinTutorDuplicado() {
        Set<String> cursosConTutor = grupoRepository.findByActivoTrue().stream()
                .filter(g -> g.getTutor() != null)
                .map(g -> claveCursoGrupo(g.getCurso(), g.getLetra()))
                .collect(Collectors.toSet());

        return grupoRepository.findByActivoTrue().stream()
                .filter(g -> g.getTutor() == null)
                .filter(g -> !cursosConTutor.contains(claveCursoGrupo(g.getCurso(), g.getLetra())))
                .findFirst();
    }

    private boolean existeOtroTutorEnMismoCurso(Grupo grupoActual, String emailTutor) {
        String clave = claveCursoGrupo(grupoActual.getCurso(), grupoActual.getLetra());
        return grupoRepository.findByActivoTrue().stream()
                .filter(g -> g.getTutor() != null)
                .filter(g -> g.getId() == null || !g.getId().equals(grupoActual.getId()))
                .anyMatch(g -> claveCursoGrupo(g.getCurso(), g.getLetra()).equals(clave)
                        && !g.getTutor().getEmail().equalsIgnoreCase(emailTutor));
    }

    private boolean cursoSinTutorAsignado(String claveCursoGrupo) {
        return grupoRepository.findByActivoTrue().stream()
                .filter(g -> g.getTutor() != null)
                .noneMatch(g -> claveCursoGrupo(g.getCurso(), g.getLetra()).equals(claveCursoGrupo));
    }

    private String claveCursoGrupo(String curso, String letra) {
        String c = curso == null ? "" : curso.trim().toUpperCase(Locale.ROOT);
        String l = letra == null ? "" : letra.trim().toUpperCase(Locale.ROOT);
        return c + "|" + l;
    }

    private String limpiar(String valor) {
        if (valor == null || valor.trim().isEmpty() || "TODOS".equalsIgnoreCase(valor.trim())) {
            return null;
        }
        return valor.trim();
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        return normalizar(valor);
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }

        String sinTildes = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return sinTildes
                .replace("º", "")
                .replace("ª", "")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
