import { Injectable } from '@nestjs/common';

export interface DeviceInfo {
  device: string;
  browser: string;
  operatingSystem: string;
}

@Injectable()
export class DeviceService {
  getDeviceInfo(userAgent: string): DeviceInfo {
    if (!userAgent) {
      return {
        device: 'Unknown',
        browser: 'Unknown',
        operatingSystem: 'Unknown',
      };
    }

    const ua = userAgent.toLowerCase();

    // Detect device
    let device = 'Desktop';
    if (ua.includes('mobile') || ua.includes('android') || ua.includes('iphone') || ua.includes('ipad')) {
      device = 'Mobile';
    } else if (ua.includes('tablet') || ua.includes('ipad')) {
      device = 'Tablet';
    }

    // Detect browser
    let browser = 'Unknown';
    if (ua.includes('chrome')) {
      browser = 'Chrome';
    } else if (ua.includes('firefox')) {
      browser = 'Firefox';
    } else if (ua.includes('safari') && !ua.includes('chrome')) {
      browser = 'Safari';
    } else if (ua.includes('edge')) {
      browser = 'Edge';
    } else if (ua.includes('opera')) {
      browser = 'Opera';
    } else if (ua.includes('ie') || ua.includes('trident')) {
      browser = 'Internet Explorer';
    }

    // Detect operating system
    let operatingSystem = 'Unknown';
    if (ua.includes('windows')) {
      operatingSystem = 'Windows';
    } else if (ua.includes('mac os')) {
      operatingSystem = 'macOS';
    } else if (ua.includes('linux')) {
      operatingSystem = 'Linux';
    } else if (ua.includes('android')) {
      operatingSystem = 'Android';
    } else if (ua.includes('ios') || ua.includes('iphone') || ua.includes('ipad')) {
      operatingSystem = 'iOS';
    }

    return {
      device,
      browser,
      operatingSystem,
    };
  }

  getDeviceIcon(device: string): string {
    switch (device.toLowerCase()) {
      case 'mobile':
        return '📱';
      case 'tablet':
        return '📱';
      case 'desktop':
        return '💻';
      default:
        return '🖥️';
    }
  }

  getBrowserIcon(browser: string): string {
    switch (browser.toLowerCase()) {
      case 'chrome':
        return '🌐';
      case 'firefox':
        return '🦊';
      case 'safari':
        return '🌐';
      case 'edge':
        return '🌐';
      case 'opera':
        return '🌐';
      case 'internet explorer':
        return '🌐';
      default:
        return '🌐';
    }
  }

  getOSIcon(operatingSystem: string): string {
    switch (operatingSystem.toLowerCase()) {
      case 'windows':
        return '🪟';
      case 'macos':
        return '🍎';
      case 'linux':
        return '🐧';
      case 'android':
        return '🤖';
      case 'ios':
        return '🍎';
      default:
        return '💻';
    }
  }
} 