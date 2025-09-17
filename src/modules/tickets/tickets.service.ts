import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Ticket, TicketStatus, TicketPriority } from './entities/ticket.entity';
import { CreateTicketDto } from './dto/create-ticket.dto';
import { UpdateTicketDto } from './dto/update-ticket.dto';
import { User } from '../users/entities/user.entity';
import { Logger } from '@nestjs/common';

@Injectable()
export class TicketsService {
  private readonly logger = new Logger(TicketsService.name);

  constructor(
    @InjectRepository(Ticket)
    private ticketRepository: Repository<Ticket>,
    @InjectRepository(User)
    private userRepository: Repository<User>,
  ) {}

  async create(createTicketDto: CreateTicketDto, createdBy: User): Promise<Ticket> {
    const ticketData = {
      title: createTicketDto.title,
      description: createTicketDto.description,
      status: createTicketDto.status || 'open' as TicketStatus,
      priority: createTicketDto.priority || 'medium' as TicketPriority,
      category: createTicketDto.category,
      subcategory: createTicketDto.subcategory,
      createdBy,
    };

    const ticket = this.ticketRepository.create(ticketData);

    if (createTicketDto.assignedTo) {
      const assignedUser = await this.userRepository.findOne({
        where: { id: createTicketDto.assignedTo }
      });
      if (assignedUser) {
        ticket.assignedTo = assignedUser;
      }
    }

    const savedTicket = await this.ticketRepository.save(ticket);
    this.logger.log(`✅ Ticket creado: ${savedTicket.title} por usuario ${createdBy.id}`);
    
    return savedTicket;
  }

  async findAll(user: User): Promise<Ticket[]> {
    // Los operadores solo pueden ver tickets asignados a ellos o creados por ellos
    if (user.role === 'operador') {
      return this.ticketRepository.find({
        where: [
          { createdBy: { id: user.id } },
          { assignedTo: { id: user.id } }
        ],
        relations: ['createdBy', 'assignedTo'],
        order: { createdAt: 'DESC' },
      });
    }

    // Otros roles pueden ver todos los tickets
    return this.ticketRepository.find({
      relations: ['createdBy', 'assignedTo'],
      order: { createdAt: 'DESC' },
    });
  }

  async findOne(id: number, user: User): Promise<Ticket> {
    const ticket = await this.ticketRepository.findOne({
      where: { id },
      relations: ['createdBy', 'assignedTo'],
    });

    if (!ticket) {
      throw new NotFoundException(`Ticket con ID ${id} no encontrado`);
    }

    // Los operadores solo pueden ver tickets asignados a ellos o creados por ellos
    if (user.role === 'operador') {
      if (ticket.createdBy.id !== user.id && ticket.assignedTo?.id !== user.id) {
        throw new ForbiddenException('No tienes permisos para ver este ticket');
      }
    }

    return ticket;
  }

  async update(id: number, updateTicketDto: UpdateTicketDto, user: User): Promise<Ticket> {
    const ticket = await this.findOne(id, user);

    // Los operadores solo pueden actualizar tickets asignados a ellos
    if (user.role === 'operador') {
      if (ticket.assignedTo?.id !== user.id) {
        throw new ForbiddenException('No tienes permisos para actualizar este ticket');
      }
    }

    Object.assign(ticket, updateTicketDto);
    const savedTicket = await this.ticketRepository.save(ticket);
    
    this.logger.log(`✅ Ticket actualizado: ${savedTicket.title} por usuario ${user.id}`);
    
    return savedTicket;
  }

  async remove(id: number, user: User): Promise<void> {
    const ticket = await this.findOne(id, user);

    // Solo los creadores pueden eliminar tickets
    if (ticket.createdBy.id !== user.id) {
      throw new ForbiddenException('Solo el creador del ticket puede eliminarlo');
    }

    await this.ticketRepository.remove(ticket);
    this.logger.log(`🗑️ Ticket eliminado: ${ticket.title} por usuario ${user.id}`);
  }

  async getStats(user: User): Promise<any> {
    let query = this.ticketRepository.createQueryBuilder('ticket');

    // Los operadores solo ven estadísticas de sus tickets
    if (user.role === 'operador') {
      query = query.where('ticket.createdBy = :userId OR ticket.assignedTo = :userId', { userId: user.id });
    }

    const total = await query.getCount();
    const open = await query.clone().andWhere('ticket.status = :status', { status: TicketStatus.OPEN }).getCount();
    const inProgress = await query.clone().andWhere('ticket.status = :status', { status: TicketStatus.IN_PROGRESS }).getCount();
    const resolved = await query.clone().andWhere('ticket.status = :status', { status: TicketStatus.RESOLVED }).getCount();
    const closed = await query.clone().andWhere('ticket.status = :status', { status: TicketStatus.CLOSED }).getCount();

    return {
      total,
      open,
      inProgress,
      resolved,
      closed
    };
  }
}
