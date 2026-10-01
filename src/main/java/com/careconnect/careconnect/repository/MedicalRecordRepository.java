package com.careconnect.careconnect.repository;

import com.careconnect.careconnect.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalRecordRepository
        extends JpaRepository<MedicalRecord, Long> {

    List<MedicalRecord> findByPatientId(Long patientId);

    List<MedicalRecord> findByPatientIdOrderByIdDesc(Long patientId);

    List<MedicalRecord> findByDoctorId(Long doctorId);

    List<MedicalRecord> findByDoctorIdOrderByIdDesc(Long doctorId);

    List<MedicalRecord> findAllByOrderByIdDesc();
}