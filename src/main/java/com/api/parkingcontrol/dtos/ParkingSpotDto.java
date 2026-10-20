package com.api.parkingcontrol.dtos;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ParkingSpotDto {

    // id e registrationDate são controlados pelo backend: se enviados pelo cliente, são ignorados
    private UUID id;

    private LocalDateTime registrationDate;

    // Os tamanhos máximos acompanham as colunas de ParkingSpotModel
    @NotBlank(message = "O número da vaga é obrigatório.")
    @Size(max = 10, message = "O número da vaga deve ter no máximo 10 caracteres.")
    private String parkingSpotNumber;

    @NotBlank(message = "A placa do veículo é obrigatória.")
    @Size(max = 7, message = "A placa do veículo deve ter no máximo 7 caracteres.")
    private String licensePlateCar;

    @NotBlank(message = "A marca do veículo é obrigatória.")
    @Size(max = 70, message = "A marca do veículo deve ter no máximo 70 caracteres.")
    private String brandCar;

    @NotBlank(message = "O modelo do veículo é obrigatório.")
    @Size(max = 70, message = "O modelo do veículo deve ter no máximo 70 caracteres.")
    private String modelCar;

    @NotBlank(message = "A cor do veículo é obrigatória.")
    @Size(max = 70, message = "A cor do veículo deve ter no máximo 70 caracteres.")
    private String colorCar;

    @NotBlank(message = "O nome do responsável é obrigatório.")
    @Size(max = 130, message = "O nome do responsável deve ter no máximo 130 caracteres.")
    private String responsibleName;

    @NotBlank(message = "O apartamento é obrigatório.")
    @Size(max = 30, message = "O apartamento deve ter no máximo 30 caracteres.")
    private String apartment;

    @NotBlank(message = "O bloco é obrigatório.")
    @Size(max = 30, message = "O bloco deve ter no máximo 30 caracteres.")
    private String block;


}
