import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import axios from 'axios';

export interface DniValidationResponse {
  status: number;
  message: string;
  success: boolean;
  data: {
    numero: string;
    codigo_verificacion: string;
    nombres: string;
    apellido_paterno: string;
    apellido_materno: string;
    nombre_completo: string;
    departamento: string;
    provincia: string;
    distrito: string;
    direccion: string;
    direccion_completa: string;
    ubigeo_reniec: string;
    ubigeo_sunat: string;
    ubigeo: string[];
    fecha_nacimiento: string;
    estado_civil: string;
    foto: string;
    sexo: string;
  };
  fuente: number;
}

@Injectable()
export class DniValidationService {
  private readonly logger = new Logger(DniValidationService.name);

  constructor(private configService: ConfigService) {}

  async validateDni(dni: string): Promise<DniValidationResponse | null> {
    try {
      const factilizaUrl = this.configService.get<string>('FACTILIZA_API_URL');
      const factilizaToken = this.configService.get<string>('FACTILIZA_API_TOKEN');

      if (!factilizaUrl || !factilizaToken) {
        this.logger.error('FACTILIZA_API_URL o FACTILIZA_API_TOKEN no configurados');
        return null;
      }

      const response = await axios.get(`${factilizaUrl}/${dni}`, {
        headers: {
          'Authorization': `Bearer ${factilizaToken}`,
          'Content-Type': 'application/json'
        },
        timeout: 10000 
      });

      if (response.data.success) {
        this.logger.log(`DNI ${dni} validado exitosamente`);
        return response.data;
      } else {
        this.logger.warn(`DNI ${dni} no encontrado en la API`);
        return null;
      }
    } catch (error) {
      this.logger.error(`Error validando DNI ${dni}:`, error.message);
      return null;
    }
  }

  formatUserData(dniData: DniValidationResponse) {
    return {
      nombres: dniData.data.nombres?.trim() || '',
      apellidos: `${dniData.data.apellido_paterno || ''} ${dniData.data.apellido_materno || ''}`.trim(),
      nombreCompleto: dniData.data.nombre_completo?.trim() || '',
      departamento: dniData.data.departamento || '',
      provincia: dniData.data.provincia || '',
      distrito: dniData.data.distrito || '',
      direccion: dniData.data.direccion || '',
      direccionCompleta: dniData.data.direccion_completa || ''
    };
  }
} 