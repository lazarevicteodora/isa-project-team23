import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { VideoService } from '../../services/video.service';
import { HttpEventType } from '@angular/common/http';

@Component({
  selector: 'app-video-upload',
  templateUrl: './video-upload.component.html',
  styleUrls: ['./video-upload.component.css']
})
export class VideoUploadComponent {
  
  // Form fields
  title: string = '';
  description: string = '';
  tagsInput: string = '';
  tags: string[] = [];
  latitude: number | null = null;
  longitude: number | null = null;
  scheduledFor: string = '';  // Za datetime-local input

  // Files
  thumbnailFile: File | null = null;
  videoFile: File | null = null;
  
  // UI states
  uploading: boolean = false;
  uploadProgress: number = 0;
  errorMessage: string = '';
  successMessage: string = '';
  
  // Previews
  thumbnailPreview: string | null = null;
  videoPreview: string | null = null;

  constructor(
    private videoService: VideoService,
    private router: Router
  ) {}

  // Dodaj tag
  addTag(): void {
    if (this.tagsInput.trim()) {
      const newTags = this.tagsInput.split(',').map(tag => tag.trim()).filter(tag => tag);
      this.tags = [...new Set([...this.tags, ...newTags])]; // Remove duplicates
      this.tagsInput = '';
    }
  }

  // Ukloni tag
  removeTag(tag: string): void {
    this.tags = this.tags.filter(t => t !== tag);
  }

  // Thumbnail file selected
  onThumbnailSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validacija: samo slike
      if (!file.type.startsWith('image/')) {
        this.errorMessage = 'Thumbnail mora biti slika!';
        return;
      }
      
      this.thumbnailFile = file;
      
      // Preview
      const reader = new FileReader();
      reader.onload = () => {
        this.thumbnailPreview = reader.result as string;
      };
      reader.readAsDataURL(file);
      this.errorMessage = '';
    }
  }

  // Video file selected
  onVideoSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validacija: MP4 format
      if (file.type !== 'video/mp4') {
        this.errorMessage = 'Video mora biti u MP4 formatu!';
        return;
      }
      
      // Validacija: max 200MB
      const maxSize = 200 * 1024 * 1024; // 200MB u bajtovima
      if (file.size > maxSize) {
        this.errorMessage = 'Video ne sme biti veći od 200MB!';
        return;
      }
      
      this.videoFile = file;
      
      // Preview
      const reader = new FileReader();
      reader.onload = () => {
        this.videoPreview = reader.result as string;
      };
      reader.readAsDataURL(file);
      this.errorMessage = '';
    }
  }

  // Get current location
  getCurrentLocation(): void {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.latitude = position.coords.latitude;
          this.longitude = position.coords.longitude;
          this.successMessage = 'Lokacija uspešno dobijena!';
          setTimeout(() => this.successMessage = '', 3000);
        },
        (error) => {
          this.errorMessage = 'Nije moguće dobiti lokaciju: ' + error.message;
        }
      );
    } else {
      this.errorMessage = 'Geolokacija nije podržana u vašem pretraživaču!';
    }
  }

  // Upload video
  uploadVideo(): void {
  console.log('🔥 uploadVideo() pozvan!');
  console.log('Title:', this.title);
  console.log('Description:', this.description);
  console.log('Tags:', this.tags);
  console.log('ScheduledFor:', this.scheduledFor);
  console.log('Thumbnail:', this.thumbnailFile);
  console.log('Video:', this.videoFile);
  
  // Validacija
  if (!this.title.trim()) {
    console.log('❌ Validacija pala: naslov prazan');
    this.errorMessage = 'Naslov je obavezan!';
    return;
  }
  
  if (!this.description.trim()) {
    console.log('❌ Validacija pala: opis prazan');
    this.errorMessage = 'Opis je obavezan!';
    return;
  }
  
  if (this.tags.length === 0) {
    console.log('❌ Validacija pala: nema tagova');
    this.errorMessage = 'Dodajte bar jedan tag!';
    return;
  }
  
  if (!this.thumbnailFile) {
    console.log('❌ Validacija pala: nema thumbnail-a');
    this.errorMessage = 'Thumbnail je obavezan!';
    return;
  }
  
  if (!this.videoFile) {
    console.log('❌ Validacija pala: nema videa');
    this.errorMessage = 'Video je obavezan!';
    return;
  }

  console.log('✅ Sve validacije prošle, pripremam FormData...');

  // Pripremi FormData
  const formData = new FormData();
  formData.append('title', this.title);
  formData.append('description', this.description);
  formData.append('tags', this.tags.join(',')); // ← PROMENI OVO! Ne JSON.stringify!
  formData.append('thumbnail', this.thumbnailFile);
  formData.append('video', this.videoFile);
  
  if (this.latitude !== null && this.longitude !== null) {
    formData.append('latitude', this.latitude.toString());
    formData.append('longitude', this.longitude.toString());
  }

  // ========== SCHEDULED STREAMING - PAŽLJIVO SA FORMATOM ==========
 // KORISTI OVO:
if (this.scheduledFor && this.scheduledFor.trim() !== '') {
  // Ukloni timezone oznaku - LocalDateTime ne podržava Z
  // Format: "2026-02-15T22:13" → ostaje isto
  const cleanDateTime = this.scheduledFor.replace(/\.\d{3}Z?$/, '');
  formData.append('scheduledFor', cleanDateTime);
  console.log('📅 Scheduled:', cleanDateTime);
} else {
    console.log('📹 Običan video (nije zakazan)');
  }


  // Upload
  this.uploading = true;
  this.uploadProgress = 0;
  this.errorMessage = '';

  this.videoService.uploadVideo(formData).subscribe({
    next: (event) => {
      console.log('📥 Event primljen:', event);
      
      if (event.type === HttpEventType.UploadProgress) {
        // Progress bar update
        this.uploadProgress = Math.round(100 * event.loaded / (event.total || 1));
        console.log('📊 Progress:', this.uploadProgress + '%');
      } else if (event.type === HttpEventType.Response) {
        // Upload completed
        console.log('✅ Upload završen!', event.body);
        this.successMessage = 'Video uspešno uploadovan! 🎉';
        setTimeout(() => {
          this.router.navigate(['/home']);
        }, 2000);
      }
    },
    error: (error) => {
      console.error('❌ Upload greška:', error);
      this.uploading = false;
      this.errorMessage = 'Greška pri uploadu: ' + (error.error?.message || error.message);
    }
  });
}

  // Reset form
  resetForm(): void {
    this.title = '';
    this.description = '';
    this.tags = [];
    this.tagsInput = '';
    this.latitude = null;
    this.longitude = null;
    this.scheduledFor = '';  // ← DODAJ OVO
    this.thumbnailFile = null;
    this.videoFile = null;
    this.thumbnailPreview = null;
    this.videoPreview = null;
    this.uploading = false;
    this.uploadProgress = 0;
    this.errorMessage = '';
    this.successMessage = '';
  }
}