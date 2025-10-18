package com.backend.perfumes.services;

import com.backend.perfumes.model.Perfume;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class PerfumeService {
    @Transactional
    public Perfume crearPerfume(ActividadCreateDto dto, String emailUsuario) {
        if (dto.getMaxEstudiantes() == null || dto.getMaxEstudiantes() < 5) {
            throw new BusinessException("La capacidad mínima es de 5 estudiantes");
        }



        Actividad actividad = new Actividad();
        actividad.setNombre(dto.getNombre());
        actividad.setFechaInicio(dto.getFechaInicio());
        actividad.setFechaFin(dto.getFechaFin());
        actividad.setMaxEstudiantes(dto.getMaxEstudiantes());
        actividad.setUbicacion(ubicacion);
        actividad.setInstructor(instructor);


        Actividad actividadGuardada = actividadRepository.save(actividad);

        auditoriaService.registrarAccion(
                emailUsuario,
                TipoAccion.CREACION,
                "Actividad creada: " + actividad.getNombre(),
                actividadGuardada.getId()
        );

        return actividadGuardada;
    }
}
