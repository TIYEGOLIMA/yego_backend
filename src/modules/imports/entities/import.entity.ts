import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, UpdateDateColumn, ManyToOne, JoinColumn } from 'typeorm';
import { User } from '../../users/entities/user.entity';

@Entity('imports')
export class Import {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'user_id' })
  userId: number;

  @Column({ type: 'varchar', length: 255 })
  filename: string;

  @Column({ type: 'varchar', length: 20, default: 'pending' })
  status: 'pending' | 'processing' | 'completed' | 'failed';

  @Column({ name: 'total_rows', type: 'int', default: 0 })
  totalRows: number;

  @Column({ name: 'processed_rows', type: 'int', default: 0 })
  processedRows: number;

  @Column({ name: 'success_rows', type: 'int', default: 0 })
  successRows: number;

  @Column({ name: 'error_rows', type: 'int', default: 0 })
  errorRows: number;

  @Column({ type: 'jsonb', nullable: true })
  errors: Record<string, any>;

  @Column({ type: 'jsonb', nullable: true })
  preview: any[];

  @Column({ type: 'varchar', length: 50 })
  type: 'users' | 'roles' | 'permissions';

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;

  @ManyToOne(() => User, user => user.id)
  @JoinColumn({ name: 'user_id' })
  user: User;
} 