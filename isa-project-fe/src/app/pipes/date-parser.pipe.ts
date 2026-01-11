import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'parseDate'
})
export class ParseDatePipe implements PipeTransform {
  transform(value: any): Date | null {
    if (!value) return null;
    
    // Ako je već Date objekat
    if (value instanceof Date) return value;
    
    // Ako je string
    if (typeof value === 'string') {
      // Ako je array string "[2026,1,9,...]"
      if (value.startsWith('[')) {
        const parts = value.replace('[', '').replace(']', '').split(',').map(Number);
        return new Date(parts[0], parts[1] - 1, parts[2], parts[3], parts[4], parts[5]);
      }
      // Ako je ISO string
      return new Date(value);
    }
    
    // Ako je array
    if (Array.isArray(value)) {
      return new Date(value[0], value[1] - 1, value[2], value[3], value[4], value[5]);
    }
    
    return null;
  }
}