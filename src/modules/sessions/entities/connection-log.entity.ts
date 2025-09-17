import { Entity, PrimaryGeneratedColumn, Column, ManyToOne, JoinColumn, CreateDateColumn } from 'typeorm';
import { User } from '../../users/entities/user.entity';
import { Session } from './session.entity';

export enum ConnectionAction {
  LOGIN = 'login',
  LOGOUT = 'logout',
  TIMEOUT = 'timeout',
  FORCED_LOGOUT = 'forced_logout'
}

@Entity('connection_logs')
export class ConnectionLog {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ nullable: true })
  user_id: number;

  @Column({ nullable: true })
  session_id: number;

  @Column({
    type: 'enum',
    enum: ConnectionAction,
    default: ConnectionAction.LOGIN
  })
  action: ConnectionAction;

  @Column({ type: 'inet', nullable: true })
  ip_address: string;

  @Column({ type: 'text', nullable: true })
  user_agent: string;

  @Column({ length: 100, nullable: true })
  device: string;

  @Column({ length: 100, nullable: true })
  browser: string;

  @Column({ length: 100, nullable: true })
  operating_system: string;

  @Column({ length: 100, nullable: true })
  city: string;

  @Column({ length: 100, nullable: true })
  region: string;

  @Column({ length: 100, nullable: true })
  country: string;

  @Column({ length: 10, nullable: true })
  country_code: string;

  @Column({ type: 'decimal', precision: 10, scale: 7, nullable: true })
  latitude: number;

  @Column({ type: 'decimal', precision: 10, scale: 7, nullable: true })
  longitude: number;

  @Column({ length: 50, nullable: true })
  timezone: string;

  @Column({ length: 200, nullable: true })
  isp: string;

  @Column({ length: 200, nullable: true })
  organization: string;

  @Column({ type: 'int', nullable: true })
  session_duration: number; // duración en segundos

  @Column({ length: 50, nullable: true })
  role_name: string;

  @CreateDateColumn()
  created_at: Date;

  // Relaciones
  @ManyToOne(() => User, { onDelete: 'SET NULL' })
  @JoinColumn({ name: 'user_id' })
  user: User;

  @ManyToOne(() => Session, { onDelete: 'SET NULL' })
  @JoinColumn({ name: 'session_id' })
  session: Session;
} 