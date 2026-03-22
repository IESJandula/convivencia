package com.iesjandula.convivencia.service;

import com.iesjandula.convivencia.entity.Grupo;
import com.iesjandula.convivencia.entity.ParteDisciplinario;
import com.iesjandula.convivencia.repository.GrupoRepository;
import com.iesjandula.convivencia.repository.ParteDisciplinarioRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.text.Normalizer;
import java.util.stream.Collectors;

@Service
public class MonitorizacionService {

    private final GrupoRepository grupoRepository;
    private final ParteDisciplinarioRepository parteRepository;

    public MonitorizacionService(GrupoRepository grupoRepository,
                                 ParteDisciplinarioRepository parteRepository) {
        this.grupoRepository = grupoRepository;
        this.parteRepository = parteRepository;
    }

    public Optional<Grupo> obtenerGrupoTutoria(String tutorEmail) {
        return grupoRepository.findByTutorEmailAndActivoTrue(tutorEmail);
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
