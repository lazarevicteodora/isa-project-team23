import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { HomeComponent } from './components/home/home.component';
import { ActivateComponent } from './components/activate/activate.component';
import { LandingComponent } from './components/landing/landing.component';
import { AuthGuard } from '../app/guards/auth.guard'; 
import { VideoUploadComponent } from './components/video-upload/video-upload.component';
import { VideoDetailComponent } from './components/video-detail/video-detail.component';
import { ProfileComponent } from './components/profile/profile.component'; 


const routes: Routes = [
  { path: '', component: LandingComponent },  
  { path: 'login', component: LoginComponent },
  { path: '', component: LandingComponent },  
  { path: 'home', component: LandingComponent }, 
  { path: 'register', component: RegisterComponent },
  { 
    path: 'upload',                              
    component: VideoUploadComponent,
    canActivate: [AuthGuard]                     
  },
  { 
    path: 'video/:id',                  
    component: VideoDetailComponent 
  },
  { 
    path: 'profile/:id',           
    component: ProfileComponent 
  },
  { path: 'activate/:token', component: ActivateComponent },
  { path: '**', redirectTo: '' }  
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
