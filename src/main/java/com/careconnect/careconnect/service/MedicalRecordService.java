package com.careconnect.careconnect.service;

import com.careconnect.careconnect.model.MedicalRecord;
import com.careconnect.careconnect.repository.MedicalRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
    }

    public MedicalRecord saveRecord(MedicalRecord record) {
        return medicalRecordRepository.save(record);
    }

    public List<MedicalRecord> getPatientRecords(Long patientId) {
        return medicalRecordRepository.findByPatientIdOrderByIdDesc(patientId);
    }

    public List<MedicalRecord> getAllRecords() {
        return medicalRecordRepository.findAllByOrderByIdDesc();
    }
}