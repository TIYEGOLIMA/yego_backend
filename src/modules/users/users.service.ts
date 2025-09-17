import { Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Like } from 'typeorm';
import { User } from './entities/user.entity';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import * as bcrypt from 'bcryptjs';

@Injectable()
export class UsersService {
  constructor(
    @InjectRepository(User)
    private userRepository: Repository<User>,
  ) {}

  async create(createUserDto: CreateUserDto): Promise<User> {
    // Check if user already exists
    const existingUser = await this.userRepository.findOne({
      where: [
        { username: createUserDto.username },
        { email: createUserDto.email },
      ],
    });

    if (existingUser) {
      throw new ConflictException('El usuario o email ya existe');
    }

    // Hash password
    const saltRounds = 12;
    const passwordHash = await bcrypt.hash(createUserDto.password, saltRounds);

    // Create user
    const user = this.userRepository.create({
      username: createUserDto.username,
      email: createUserDto.email,
      name: createUserDto.name || createUserDto.username,
      password: passwordHash,
      role: createUserDto.role || 'usuario',
      moduleId: createUserDto.moduleId,
      active: true,
    });

    return this.userRepository.save(user);
  }

  async findAll(page: number = 1, limit: number = 10, search?: string, active: boolean | null = true): Promise<User[]> {
    const query = this.userRepository.createQueryBuilder('user');

    if (search) {
      query.where(
        'user.username LIKE :search OR user.email LIKE :search OR user.name LIKE :search',
        { search: `%${search}%` }
      );
    }

    // Filtrar por activos por defecto, pero permitir traer todos si active es null
    if (typeof active === 'boolean') {
      query.andWhere('user.active = :active', { active });
    }

    const users = await query
      .skip((page - 1) * limit)
      .take(limit)
      .getMany();

    return users;
  }

  async findOne(id: number): Promise<User> {
    const user = await this.userRepository.findOne({
      where: { id },
    });

    if (!user) {
      throw new NotFoundException('Usuario no encontrado');
    }

    return user;
  }

  async findByUsername(username: string): Promise<User> {
    const user = await this.userRepository.findOne({
      where: { username },
    });

    if (!user) {
      throw new NotFoundException('Usuario no encontrado');
    }

    return user;
  }

  async update(id: number, updateUserDto: UpdateUserDto): Promise<User> {
    const user = await this.findOne(id);

    // Check if new username/email conflicts with existing users
    if (updateUserDto.username && updateUserDto.username !== user.username) {
      const existingUser = await this.userRepository.findOne({
        where: { username: updateUserDto.username },
      });
      if (existingUser) {
        throw new ConflictException('El nombre de usuario ya existe');
      }
    }

    if (updateUserDto.email && updateUserDto.email !== user.email) {
      const existingUser = await this.userRepository.findOne({
        where: { email: updateUserDto.email },
      });
      if (existingUser) {
        throw new ConflictException('El email ya existe');
      }
    }

    // Hash password if provided
    if (updateUserDto.password) {
      const saltRounds = 12;
      user.password = await bcrypt.hash(updateUserDto.password, saltRounds);
      delete updateUserDto.password;
    }

    // Update user properties
    Object.assign(user, updateUserDto);
    return this.userRepository.save(user);
  }

  async remove(id: number): Promise<void> {
    const user = await this.findOne(id);
    await this.userRepository.softDelete(id);
  }

  async activate(id: number): Promise<User> {
    const user = await this.findOne(id);
    user.active = true;
    return this.userRepository.save(user);
  }

  async deactivate(id: number): Promise<User> {
    const user = await this.findOne(id);
    user.active = false;
    return this.userRepository.save(user);
  }

  async changePassword(id: number, newPassword: string): Promise<void> {
    const user = await this.findOne(id);
    const saltRounds = 12;
    user.password = await bcrypt.hash(newPassword, saltRounds);
    await this.userRepository.save(user);
  }

  async validatePassword(password: string): Promise<boolean> {
    // Check for weak passwords
    const weakPasswords = ['123456', 'admin', 'password', '123456789', 'qwerty'];
    if (weakPasswords.includes(password.toLowerCase())) {
      return false;
    }

    // Check for minimum requirements
    const hasUpperCase = /[A-Z]/.test(password);
    const hasLowerCase = /[a-z]/.test(password);
    const hasNumbers = /\d/.test(password);
    const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(password);
    const isLongEnough = password.length >= 8;

    return hasUpperCase && hasLowerCase && hasNumbers && hasSpecialChar && isLongEnough;
  }
} 