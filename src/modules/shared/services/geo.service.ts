import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

export interface GeoLocation {
  ip: string;
  city: string;
  region: string;
  country: string;
  countryCode: string;
  latitude: number;
  longitude: number;
  timezone: string;
  isp: string;
  org: string;
}

@Injectable()
export class GeoService {
  private readonly logger = new Logger(GeoService.name);
  private readonly cache = new Map<string, GeoLocation>();
  private readonly cacheTimeout = 24 * 60 * 60 * 1000; // 24 hours

  constructor(private configService: ConfigService) {}

  async getLocationByIP(ip: string): Promise<GeoLocation | null> {
    try {
      // Skip localhost and private IPs
      if (this.isPrivateIP(ip)) {
        return this.getDefaultLocation(ip);
      }

      // Check cache first
      const cached = this.cache.get(ip);
      if (cached) {
        return cached;
      }

      // Use ipapi.co (free tier)
      const response = await fetch(`https://ipapi.co/${ip}/json/`);
      
      if (!response.ok) {
        this.logger.warn(`Failed to get location for IP ${ip}: ${response.statusText}`);
        return this.getDefaultLocation(ip);
      }

      const data = await response.json();

      if (data.error) {
        this.logger.warn(`IP API error for ${ip}: ${data.reason}`);
        return this.getDefaultLocation(ip);
      }

      const location: GeoLocation = {
        ip: data.ip || ip,
        city: data.city || 'Unknown',
        region: data.region || 'Unknown',
        country: data.country_name || 'Unknown',
        countryCode: data.country_code || 'XX',
        latitude: parseFloat(data.latitude) || 0,
        longitude: parseFloat(data.longitude) || 0,
        timezone: data.timezone || 'UTC',
        isp: data.org || 'Unknown',
        org: data.org || 'Unknown',
      };

      // Cache the result
      this.cache.set(ip, location);
      
      // Clear cache after timeout
      setTimeout(() => {
        this.cache.delete(ip);
      }, this.cacheTimeout);

      this.logger.log(`📍 Location obtained for IP ${ip}: ${location.city}, ${location.country}`);
      
      return location;

    } catch (error) {
      this.logger.error(`Error getting location for IP ${ip}:`, error);
      return this.getDefaultLocation(ip);
    }
  }

  private isPrivateIP(ip: string): boolean {
    const privateRanges = [
      /^10\./,
      /^172\.(1[6-9]|2[0-9]|3[0-1])\./,
      /^192\.168\./,
      /^127\./,
      /^::1$/,
      /^fc00:/,
      /^fe80:/,
    ];

    return privateRanges.some(range => range.test(ip));
  }

  private getDefaultLocation(ip: string): GeoLocation {
    return {
      ip,
      city: 'Local',
      region: 'Local',
      country: 'Local',
      countryCode: 'XX',
      latitude: 0,
      longitude: 0,
      timezone: 'UTC',
      isp: 'Local Network',
      org: 'Local Network',
    };
  }

  async getClientIP(request: any): Promise<string> {
    // Check various headers for real IP
    const headers = [
      'x-forwarded-for',
      'x-real-ip',
      'x-client-ip',
      'cf-connecting-ip', // Cloudflare
      'x-forwarded',
      'forwarded-for',
      'forwarded',
    ];

    for (const header of headers) {
      const value = request.headers[header];
      if (value) {
        // Handle comma-separated IPs (take the first one)
        const ip = value.split(',')[0].trim();
        if (ip && this.isValidIP(ip)) {
          return ip;
        }
      }
    }

    // Fallback to connection remote address
    return request.connection?.remoteAddress || 
           request.socket?.remoteAddress || 
           request.ip || 
           '127.0.0.1';
  }

  private isValidIP(ip: string): boolean {
    const ipv4Regex = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
    const ipv6Regex = /^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$/;
    
    return ipv4Regex.test(ip) || ipv6Regex.test(ip);
  }

  clearCache(): void {
    this.cache.clear();
    this.logger.log('🧹 Geolocation cache cleared');
  }

  getCacheStats(): { size: number; entries: string[] } {
    return {
      size: this.cache.size,
      entries: Array.from(this.cache.keys()),
    };
  }
} 