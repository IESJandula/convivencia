package com.iesjandula.convivencia.service;

import com.iesjandula.convivencia.dto.CrearExpulsionRequestDto;
import com.iesjandula.convivencia.dto.CrearExpulsionResponseDto;
import com.iesjandula.convivencia.dto.ExpulsionPdfItemDto;
import com.iesjandula.convivencia.entity.*;
import com.iesjandula.convivencia.repository.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpulsionService {
    private static final String TEXTO_TAREA_AUTOGENERADA = "tarea pendiente por expulsión del alumno";

    private final AlumnoRepository alumnoRepository;
    private final ProfesorRepository profesorRepository;
    private final ParteDisciplinarioRepository parteRepository;
    private final ExpulsionRepository expulsionRepository;
    private final ParteExpulsionRepository parteExpulsionRepository;
    private final TareaExpulsionRepository tareaExpulsionRepository;
    private final GrupoRepository grupoRepository;
    private final ImparteRepository imparteRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ExpulsionService(AlumnoRepository alumnoRepository,
                            ProfesorRepository profesorRepository,
                            ParteDisciplinarioRepository parteRepository,
                            ExpulsionRepository expulsionRepository,
                            ParteExpulsionRepository parteExpulsionRepository,
                            TareaExpulsionRepository tareaExpulsionRepository,
                            GrupoRepository grupoRepository,
                            ImparteRepository imparteRepository) {
        this.alumnoRepository = alumnoRepository;
        this.profesorRepository = profesorRepository;
        this.parteRepository = parteRepository;
        this.expulsionRepository = expulsionRepository;
        this.parteExpulsionRepository = parteExpulsionRepository;
        this.tareaExpulsionRepository = tareaExpulsionRepository;
        this.grupoRepository = grupoRepository;
        this.imparteRepository = imparteRepository;
    }

    public List<ParteDisciplinario> obtenerPartesActivosAlumno(Integer alumnoId) {
        return parteRepository.findByAlumnoIdAndActivoTrue(alumnoId).stream()
                .filter(parte -> parte.getEstadoComputo() == ParteDisciplinario.EstadoComputo.ACTIVO)
                .sorted(Comparator.comparing(ParteDisciplinario::getFecha).reversed())
                .collect(Collectors.toList());
    }

    public List<ExpulsionPdfItemDto> listarExpulsionesParaPdf() {
        return expulsionRepository.findByActivoTrue().stream()
                .sorted(Comparator.comparing(Expulsion::getFechaCreacion, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(expulsion -> {
                    Alumno alumno = expulsion.getAlumno();
                    String nombreCompleto = "";
                    String curso = "";
                    String grupo = "";

                    if (alumno != null) {
                        nombreCompleto = (Objects.requireNonNullElse(alumno.getNombre(), "") + " "
                                + Objects.requireNonNullElse(alumno.getApellidos(), "")).trim();
                        curso = Objects.requireNonNullElse(alumno.getCurso(), "");
                        grupo = Objects.requireNonNullElse(alumno.getGrupo(), "");
                    }

                        List<TareaExpulsion> tareasExpulsion = tareaExpulsionRepository.findByExpulsionId(expulsion.getId());
                        int tareasTotales = tareasExpulsion.size();
                        int tareasCompletadas = (int) tareasExpulsion.stream()
                            .filter(tarea -> tarea.getEstado() == TareaExpulsion.Estado.COMPLETADA)
                            .filter(tarea -> esActividadProfesorValida(tarea.getDescripcionTarea()))
                            .count();

                    return new ExpulsionPdfItemDto(
                            expulsion.getId(),
                            alumno != null ? alumno.getId() : null,
                            nombreCompleto,
                            curso,
                            grupo,
                            puedeGenerarCartaExpulsion(expulsion.getId()),
                            tareasCompletadas,
                            tareasTotales
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public CrearExpulsionResponseDto crearExpulsion(CrearExpulsionRequestDto request) {
        validarRequest(request);

        Alumno alumno = alumnoRepository.findById(request.getAlumnoId())
                .orElseThrow(() -> new RuntimeException("Alumno no encontrado"));
        if (!Boolean.TRUE.equals(alumno.getActivo())) {
            throw new RuntimeException("Alumno no disponible");
        }

        Profesor jefatura = profesorRepository.findByEmailAndActivoTrue(request.getJefaturaEmail())
                .orElseGet(() -> profesorRepository.findByEmailNormalized(request.getJefaturaEmail())
                        .orElseThrow(() -> new RuntimeException("Usuario de jefatura no encontrado")));

        if (jefatura.getRol() != Profesor.Rol.JEFATURA && jefatura.getRol() != Profesor.Rol.ADMIN) {
            throw new RuntimeException("Solo Jefatura o Admin puede crear expulsiones");
        }

        List<ParteDisciplinario> partesSeleccionados = parteRepository.findAllById(request.getParteIds());
        if (partesSeleccionados.size() != request.getParteIds().size()) {
            throw new RuntimeException("Hay partes seleccionados que no existen");
        }

        for (ParteDisciplinario parte : partesSeleccionados) {
            if (!Boolean.TRUE.equals(parte.getActivo())) {
                throw new RuntimeException("Se ha seleccionado un parte inactivo");
            }
            if (!Objects.equals(parte.getAlumno().getId(), alumno.getId())) {
                throw new RuntimeException("Todos los partes seleccionados deben pertenecer al alumno filtrado");
            }
            if (parte.getEstadoComputo() != ParteDisciplinario.EstadoComputo.ACTIVO) {
                throw new RuntimeException("Solo se pueden computar partes en estado ACTIVO");
            }
        }

        Expulsion expulsion = new Expulsion();
        expulsion.setAlumno(alumno);
        expulsion.setJefatura(jefatura);
        expulsion.setFechaInicio(request.getFechaInicio());
        expulsion.setFechaFin(request.getFechaFin());
        expulsion.setActivo(true);
        expulsion = expulsionRepository.save(expulsion);

        for (ParteDisciplinario parte : partesSeleccionados) {
            parte.setEstadoComputo(ParteDisciplinario.EstadoComputo.COMPUTADO);
            parteRepository.save(parte);

            ParteExpulsion parteExpulsion = new ParteExpulsion();
            parteExpulsion.setExpulsion(expulsion);
            parteExpulsion.setParte(parte);
            parteExpulsionRepository.save(parteExpulsion);
        }

        int tareasGeneradas = generarTareasPendientes(expulsion, alumno);

        return new CrearExpulsionResponseDto(
                expulsion.getId(),
                partesSeleccionados.size(),
                tareasGeneradas
        );
    }

    private void validarRequest(CrearExpulsionRequestDto request) {
        if (request == null) {
            throw new RuntimeException("Solicitud no válida");
        }
        if (request.getAlumnoId() == null) {
            throw new RuntimeException("Debe indicar el alumno");
        }
        if (request.getJefaturaEmail() == null || request.getJefaturaEmail().trim().isEmpty()) {
            throw new RuntimeException("Debe indicar el usuario de jefatura");
        }
        if (request.getParteIds() == null || request.getParteIds().isEmpty()) {
            throw new RuntimeException("Debe seleccionar al menos un parte");
        }
        if (request.getFechaInicio() == null || request.getFechaFin() == null) {
            throw new RuntimeException("Debe indicar fecha de inicio y fin");
        }
        if (request.getFechaFin().isBefore(request.getFechaInicio())) {
            throw new RuntimeException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }

    private int generarTareasPendientes(Expulsion expulsion, Alumno alumno) {
        Optional<Grupo> grupoAlumnoOpt = resolverGrupoAlumno(alumno);
        List<Imparte> imparteList = grupoAlumnoOpt
                .map(grupo -> imparteRepository.findByGrupoIdAndActivoTrue(grupo.getId()))
                .orElseGet(Collections::emptyList);

        Map<String, Imparte> porProfesor = new LinkedHashMap<>();
        for (Imparte imparte : imparteList) {
            if (imparte.getProfesor() != null && Boolean.TRUE.equals(imparte.getProfesor().getActivo())) {
                String asignatura = imparte.getAsignatura();
                if (asignatura == null || asignatura.trim().isEmpty()) {
                    imparte.setAsignatura(inventarAsignaturaParaProfesor(imparte.getProfesor().getEmail()));
                    imparte = imparteRepository.save(imparte);
                }
                porProfesor.putIfAbsent(imparte.getProfesor().getEmail(), imparte);
            }
        }

        List<ParteDisciplinario> partesAlumno = parteRepository.findByAlumnoIdAndActivoTrue(alumno.getId());
        for (ParteDisciplinario parte : partesAlumno) {
            Profesor profesor = parte.getProfesor();
            if (profesor == null || !Boolean.TRUE.equals(profesor.getActivo())) {
                continue;
            }

            if (porProfesor.containsKey(profesor.getEmail())) {
                continue;
            }

            String asignaturaInventada = inventarAsignaturaParaProfesor(profesor.getEmail());

            if (grupoAlumnoOpt.isPresent()) {
                Imparte nuevoImparte = new Imparte();
                nuevoImparte.setProfesor(profesor);
                nuevoImparte.setGrupo(grupoAlumnoOpt.get());
                nuevoImparte.setAsignatura(asignaturaInventada);
                nuevoImparte.setActivo(true);
                nuevoImparte = imparteRepository.save(nuevoImparte);
                porProfesor.put(profesor.getEmail(), nuevoImparte);
            } else {
                Imparte imparteFallback = new Imparte();
                imparteFallback.setProfesor(profesor);
                imparteFallback.setAsignatura(asignaturaInventada);
                imparteFallback.setActivo(true);
                porProfesor.put(profesor.getEmail(), imparteFallback);
            }
        }

        int creadas = 0;
        for (Imparte imparte : porProfesor.values()) {
            if (imparte.getProfesor() == null) {
                continue;
            }

            TareaExpulsion tarea = new TareaExpulsion();
            tarea.setExpulsion(expulsion);
            tarea.setProfesor(imparte.getProfesor());
            String asignatura = resolverAsignaturaProfesor(imparte.getProfesor().getEmail(), imparte.getAsignatura());
            if (asignatura == null || asignatura.trim().isEmpty()) {
                asignatura = inventarAsignaturaParaProfesor(imparte.getProfesor().getEmail());
                if (imparte.getId() != null) {
                    imparte.setAsignatura(asignatura);
                    imparteRepository.save(imparte);
                }
            }
            tarea.setAsignatura(asignatura);
            tarea.setDescripcionTarea(null);
            tarea.setEstado(TareaExpulsion.Estado.PENDIENTE);
            tareaExpulsionRepository.save(tarea);
            creadas++;
        }

        return creadas;
    }

    private String resolverAsignaturaProfesor(String profesorEmail, String asignaturaCandidata) {
        if (esAsignaturaValida(asignaturaCandidata)) {
            return asignaturaCandidata;
        }

        List<Imparte> asignacionesProfesor = imparteRepository.findByProfesorEmailAndActivoTrue(profesorEmail);
        for (Imparte asignacion : asignacionesProfesor) {
            if (esAsignaturaValida(asignacion.getAsignatura())) {
                return asignacion.getAsignatura();
            }
        }

        return null;
    }

    private boolean esAsignaturaValida(String asignatura) {
        if (asignatura == null) {
            return false;
        }
        String valor = asignatura.trim();
        return !valor.isEmpty() && !"pendiente asignatura".equalsIgnoreCase(valor);
    }

    private Optional<Grupo> resolverGrupoAlumno(Alumno alumno) {
        List<Grupo> gruposActivos = grupoRepository.findByActivoTrue();
        String cursoAlumno = normalize(alumno.getCurso());
        String grupoAlumno = normalize(alumno.getGrupo());

        return gruposActivos.stream()
                .filter(grupo -> normalize(grupo.getCurso()).equals(cursoAlumno))
                .filter(grupo -> normalize(grupo.getLetra()).equals(grupoAlumno))
                .findFirst();
    }

    private String normalize(String valor) {
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

    private String inventarAsignaturaParaProfesor(String email) {
        List<String> materias = List.of(
                "Matemáticas",
                "Lengua",
                "Inglés",
                "Biología",
                "Geografía e Historia",
                "Física y Química",
                "Educación Física",
                "Tecnología"
        );

        int idx = Math.abs(Objects.requireNonNullElse(email, "").toLowerCase(Locale.ROOT).hashCode()) % materias.size();
        return materias.get(idx);
    }

    public byte[] generarCartaExpulsionPdf(Integer expulsionId) {
        Expulsion expulsion = expulsionRepository.findById(expulsionId)
                .orElseThrow(() -> new RuntimeException("Expulsión no encontrada"));

        List<ParteExpulsion> partesVinculados = parteExpulsionRepository.findByExpulsionId(expulsionId);
        List<TareaExpulsion> tareas = tareaExpulsionRepository.findByExpulsionId(expulsionId);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream content = new PDPageContentStream(document, page);
            float y = 780f;
            float left = 50f;
            float lineHeight = 16f;

            y = writeLine(content, "CARTA OFICIAL DE EXPULSIÓN", left, y, PDType1Font.HELVETICA_BOLD, 14);
            y -= 4;
            y = writeLine(content, "IES Jándula - Sistema de Convivencia", left, y, PDType1Font.HELVETICA, 11);
            y -= 12;

            Alumno alumno = expulsion.getAlumno();
            y = writeLine(content, "Alumno: " + safe(alumno != null ? alumno.getNombre() : "") + " " + safe(alumno != null ? alumno.getApellidos() : ""), left, y, PDType1Font.HELVETICA_BOLD, 11);
            y = writeLine(content, "Curso/Grupo: " + safe(alumno != null ? alumno.getCurso() : "") + " " + safe(alumno != null ? alumno.getGrupo() : ""), left, y, PDType1Font.HELVETICA, 11);
            y = writeLine(content, "Fecha inicio expulsión: " + DATE_FMT.format(expulsion.getFechaInicio()), left, y, PDType1Font.HELVETICA, 11);
            y = writeLine(content, "Fecha fin expulsión: " + DATE_FMT.format(expulsion.getFechaFin()), left, y, PDType1Font.HELVETICA, 11);
            y = writeLine(content, "Tramitada por: " + safe(expulsion.getJefatura() != null ? expulsion.getJefatura().getNombre() : ""), left, y, PDType1Font.HELVETICA, 11);
            y -= 12;

            y = writeLine(content, "Partes computados:", left, y, PDType1Font.HELVETICA_BOLD, 12);
            if (partesVinculados.isEmpty()) {
                y = writeLine(content, "- No hay partes asociados.", left + 10, y, PDType1Font.HELVETICA, 11);
            } else {
                for (ParteExpulsion pe : partesVinculados) {
                    ParteDisciplinario parte = pe.getParte();
                    if (parte == null || parte.getFecha() == null) {
                        continue;
                    }
                    String linea = String.format("- %s | %s | %s",
                            DATE_FMT.format(parte.getFecha()),
                            safe(parte.getGravedad() != null ? parte.getGravedad().name() : ""),
                            safe(parte.getDescripcion()));
                    y = writeWrapped(content, linea, left + 10, y, 500, lineHeight, PDType1Font.HELVETICA, 10);
                }
            }

            y -= 10;
            y = writeLine(content, "Tareas propuestas por el equipo docente:", left, y, PDType1Font.HELVETICA_BOLD, 12);
            y -= 4;

            y = drawTareasTable(content, tareas, left, y);
            content.close();

            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar el PDF de expulsión", e);
        }
    }

    private float drawTareasTable(PDPageContentStream content, List<TareaExpulsion> tareas, float x, float y) throws IOException {
        float tableWidth = 500f;
        float rowHeight = 18f;
        float col1 = 130f;
        float col2 = 100f;
        float col3 = tableWidth - col1 - col2;

        content.setFont(PDType1Font.HELVETICA_BOLD, 10);
        content.beginText();
        content.newLineAtOffset(x + 2, y);
        content.showText("Profesor");
        content.endText();
        content.beginText();
        content.newLineAtOffset(x + col1 + 2, y);
        content.showText("Asignatura");
        content.endText();
        content.beginText();
        content.newLineAtOffset(x + col1 + col2 + 2, y);
        content.showText("Tarea");
        content.endText();

        y -= rowHeight;
        content.setFont(PDType1Font.HELVETICA, 9);

        if (tareas.isEmpty()) {
            content.beginText();
            content.newLineAtOffset(x + 2, y);
            content.showText("No hay tareas registradas.");
            content.endText();
            return y - rowHeight;
        }

        for (TareaExpulsion t : tareas) {
            content.beginText();
            content.newLineAtOffset(x + 2, y);
            String nombreProfesor = (t.getProfesor() != null) ? t.getProfesor().getNombre() : "";
            content.showText(cut(nombreProfesor, 22));
            content.endText();

            content.beginText();
            content.newLineAtOffset(x + col1 + 2, y);
            content.showText(cut(safe(t.getAsignatura()), 16));
            content.endText();

            content.beginText();
            content.newLineAtOffset(x + col1 + col2 + 2, y);
            content.showText(cut(safe(t.getDescripcionTarea()), 52));
            content.endText();

            y -= rowHeight;
            if (y < 70) {
                break;
            }
        }

        return y;
    }

    private float writeLine(PDPageContentStream content,
                            String text,
                            float x,
                            float y,
                            PDType1Font font,
                            int size) throws IOException {
        content.setFont(font, size);
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - 16;
    }

    private float writeWrapped(PDPageContentStream content,
                               String text,
                               float x,
                               float y,
                               float maxWidth,
                               float lineHeight,
                               PDType1Font font,
                               int size) throws IOException {
        content.setFont(font, size);
        String[] words = safe(text).split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            float candidateWidth = font.getStringWidth(candidate) / 1000 * size;
            if (candidateWidth > maxWidth && !line.isEmpty()) {
                content.beginText();
                content.newLineAtOffset(x, y);
                content.showText(line.toString());
                content.endText();
                y -= lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }

        if (!line.isEmpty()) {
            content.beginText();
            content.newLineAtOffset(x, y);
            content.showText(line.toString());
            content.endText();
            y -= lineHeight;
        }

        return y;
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .trim();

        StringBuilder clean = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            if ((ch >= 32 && ch <= 255) || ch == '€') {
                clean.append(ch);
            } else {
                clean.append(' ');
            }
        }

        return clean.toString().replaceAll("\\s+", " ").trim();
    }

    private String cut(String value, int maxLen) {
        String text = safe(value);
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    public boolean puedeGenerarCartaExpulsion(Integer expulsionId) {
        List<TareaExpulsion> tareas = tareaExpulsionRepository.findByExpulsionId(expulsionId);
        if (tareas.isEmpty()) {
            return false;
        }

        return tareas.stream().allMatch(t ->
                t.getEstado() == TareaExpulsion.Estado.COMPLETADA
                        && esActividadProfesorValida(t.getDescripcionTarea()));
    }

    private boolean esActividadProfesorValida(String descripcionTarea) {
        if (descripcionTarea == null) {
            return false;
        }

        String texto = descripcionTarea.trim();
        if (texto.isEmpty()) {
            return false;
        }

        return !texto.toLowerCase(Locale.ROOT).startsWith(TEXTO_TAREA_AUTOGENERADA);
    }
}
