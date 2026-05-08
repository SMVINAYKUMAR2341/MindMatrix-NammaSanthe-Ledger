import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';
import { Badge } from './ui/badge';
import { Switch } from './ui/switch';
import { Separator } from './ui/separator';
import {
  Camera,
  Store,
  Phone,
  Mail,
  MapPin,
  Bell,
  Download,
  Upload,
  Trash2,
  Save,
  Edit
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { compressImage } from '../../lib/ocr-processor';
import { db } from '../../lib/database';
import { toast } from 'sonner';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogDescription
} from './ui/dialog';

interface AppProfileProps {
  onBack: () => void;
}

interface BusinessProfile {
  id: number;
  shopName: string;
  ownerName: string;
  phone: string;
  email: string;
  address: string;
  photoPath?: string;
  gstNumber?: string;
  createdAt: number;
}

export default function AppProfile({ onBack }: AppProfileProps) {
  const { t } = useTranslation();
  const [profile, setProfile] = useState<BusinessProfile>({
    id: 1,
    shopName: 'Your Shop Name',
    ownerName: 'Your Name',
    phone: '',
    email: '',
    address: 'Village Market',
    createdAt: Date.now()
  });
  const [isEditing, setIsEditing] = useState(false);
  const [editedProfile, setEditedProfile] = useState(profile);
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    const stored = localStorage.getItem('businessProfile');
    if (stored) {
      const parsed = JSON.parse(stored);
      setProfile(parsed);
      setEditedProfile(parsed);
    }
  };

  const handlePhotoCapture = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      const compressed = await compressImage(file);
      const photoPath = `business_photo_${Date.now()}`;
      localStorage.setItem(photoPath, compressed);

      const updated = { ...editedProfile, photoPath };
      setEditedProfile(updated);
      toast.success('Photo updated!');
    } catch (error) {
      console.error('Photo error:', error);
      toast.error('Failed to update photo');
    }
  };

  const handleSave = () => {
    localStorage.setItem('businessProfile', JSON.stringify(editedProfile));
    setProfile(editedProfile);
    setIsEditing(false);
    toast.success('Profile updated successfully!');
  };

  const handleCancel = () => {
    setEditedProfile(profile);
    setIsEditing(false);
  };

  const handleExportData = async () => {
    const customers = await db.customers.toArray();
    const transactions = await db.transactions.toArray();
    const data = {
      customers,
      transactions,
      exportDate: new Date().toISOString(),
      businessProfile: profile
    };

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `namma-santhe-backup-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success('Data exported successfully!');
  };

  const handleClearData = async () => {
    await db.customers.clear();
    await db.transactions.clear();
    localStorage.clear();
    toast.success('All data cleared!');
    window.location.reload();
  };

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  const getPhoto = (photoPath?: string): string | null => {
    if (!photoPath) return null;
    return localStorage.getItem(photoPath);
  };

  return (
    <div className="flex flex-col h-full bg-gradient-to-b from-purple-50 via-white to-blue-50">
      {/* Header */}
      <div className="bg-gradient-to-br from-purple-500 via-pink-500 to-red-500 text-white p-6 rounded-b-3xl shadow-lg">
        <Button variant="ghost" onClick={onBack} className="text-white hover:bg-white/20 mb-4">
          ← {t('back')}
        </Button>

        <div className="flex items-start gap-4">
          <div className="relative">
            <Avatar className="w-24 h-24 border-4 border-white shadow-xl">
              <AvatarImage src={getPhoto(editedProfile.photoPath) || undefined} />
              <AvatarFallback className="text-2xl bg-white text-purple-600 font-bold">
                {getInitials(editedProfile.shopName)}
              </AvatarFallback>
            </Avatar>
            {isEditing && (
              <>
                <input
                  type="file"
                  accept="image/*"
                  capture="environment"
                  className="hidden"
                  id="business-photo-input"
                  onChange={handlePhotoCapture}
                />
                <button
                  onClick={() => document.getElementById('business-photo-input')?.click()}
                  className="absolute bottom-0 right-0 bg-white text-purple-600 rounded-full p-2 shadow-lg hover:scale-110 transition-transform"
                  type="button"
                >
                  <Camera size={16} />
                </button>
              </>
            )}
          </div>

          <div className="flex-1">
            <h1 className="text-2xl font-bold mb-1">{profile.shopName}</h1>
            <p className="text-white/90">{profile.ownerName}</p>
            <Badge className="mt-2 bg-white/20 text-white border-white/30">
              Since {new Date(profile.createdAt).toLocaleDateString('en-IN')}
            </Badge>
          </div>

          {!isEditing && (
            <Button
              variant="ghost"
              className="text-white hover:bg-white/20 gap-2"
              onClick={() => setIsEditing(true)}
            >
              <Edit size={18} />
              <span className="text-sm">Edit</span>
            </Button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-auto p-4 space-y-4">
        {/* Business Details */}
        <Card className={isEditing ? 'border-2 border-purple-500 shadow-lg' : ''}>
          <CardHeader>
            <CardTitle className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Store size={20} className="text-purple-600" />
                Business Details
              </div>
              {isEditing && (
                <Badge variant="default" className="bg-purple-500">
                  Editing Mode
                </Badge>
              )}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <Label className="flex items-center gap-2">
                Shop Name
                {isEditing && <span className="text-xs text-purple-600">✏️ Click to edit</span>}
              </Label>
              <Input
                value={editedProfile.shopName}
                onChange={(e) => setEditedProfile({ ...editedProfile, shopName: e.target.value })}
                disabled={!isEditing}
                className={!isEditing ? 'bg-muted cursor-not-allowed' : 'border-2 border-purple-300 focus:border-purple-500'}
              />
            </div>

            <div>
              <Label className="flex items-center gap-2">
                Owner Name
                {isEditing && <span className="text-xs text-purple-600">✏️ Click to edit</span>}
              </Label>
              <Input
                value={editedProfile.ownerName}
                onChange={(e) => setEditedProfile({ ...editedProfile, ownerName: e.target.value })}
                disabled={!isEditing}
                className={!isEditing ? 'bg-muted cursor-not-allowed' : 'border-2 border-purple-300 focus:border-purple-500'}
              />
            </div>

            <div>
              <Label className="flex items-center gap-2">
                Phone
                {isEditing && <span className="text-xs text-purple-600">✏️ Click to edit</span>}
              </Label>
              <div className="relative">
                <Phone size={18} className="absolute left-3 top-3 text-muted-foreground" />
                <Input
                  value={editedProfile.phone}
                  onChange={(e) => setEditedProfile({ ...editedProfile, phone: e.target.value })}
                  disabled={!isEditing}
                  className={!isEditing ? 'bg-muted cursor-not-allowed pl-10' : 'pl-10 border-2 border-purple-300 focus:border-purple-500'}
                  placeholder="Your phone number"
                />
              </div>
            </div>

            <div>
              <Label className="flex items-center gap-2">
                Email (Optional)
                {isEditing && <span className="text-xs text-purple-600">✏️ Click to edit</span>}
              </Label>
              <div className="relative">
                <Mail size={18} className="absolute left-3 top-3 text-muted-foreground" />
                <Input
                  value={editedProfile.email || ''}
                  onChange={(e) => setEditedProfile({ ...editedProfile, email: e.target.value })}
                  disabled={!isEditing}
                  className={!isEditing ? 'bg-muted cursor-not-allowed pl-10' : 'pl-10 border-2 border-purple-300 focus:border-purple-500'}
                  placeholder="your@email.com"
                />
              </div>
            </div>

            <div>
              <Label className="flex items-center gap-2">
                Address
                {isEditing && <span className="text-xs text-purple-600">✏️ Click to edit</span>}
              </Label>
              <div className="relative">
                <MapPin size={18} className="absolute left-3 top-3 text-muted-foreground" />
                <Input
                  value={editedProfile.address}
                  onChange={(e) => setEditedProfile({ ...editedProfile, address: e.target.value })}
                  disabled={!isEditing}
                  className={!isEditing ? 'bg-muted cursor-not-allowed pl-10' : 'pl-10 border-2 border-purple-300 focus:border-purple-500'}
                  placeholder="Shop address"
                />
              </div>
            </div>

            <div>
              <Label className="flex items-center gap-2">
                GST Number (Optional)
                {isEditing && <span className="text-xs text-purple-600">✏️ Click to edit</span>}
              </Label>
              <Input
                value={editedProfile.gstNumber || ''}
                onChange={(e) => setEditedProfile({ ...editedProfile, gstNumber: e.target.value })}
                disabled={!isEditing}
                className={!isEditing ? 'bg-muted cursor-not-allowed' : 'border-2 border-purple-300 focus:border-purple-500'}
                placeholder="22AAAAA0000A1Z5"
              />
            </div>

            {isEditing && (
              <div className="flex gap-3 pt-4 border-t">
                <Button onClick={handleSave} className="flex-1 bg-gradient-to-r from-green-500 to-green-600 hover:from-green-600 hover:to-green-700 shadow-lg">
                  <Save size={18} className="mr-2" />
                  Save Changes
                </Button>
                <Button onClick={handleCancel} variant="outline" className="flex-1 border-2 hover:bg-gray-100">
                  Cancel
                </Button>
              </div>
            )}

            {!isEditing && (
              <div className="pt-4 border-t">
                <Button
                  onClick={() => setIsEditing(true)}
                  className="w-full bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600"
                >
                  <Edit size={18} className="mr-2" />
                  Edit Profile
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Settings */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Bell size={20} className="text-blue-600" />
              Settings
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium">Daily Notifications</p>
                <p className="text-sm text-muted-foreground">Get daily business summary</p>
              </div>
              <Switch
                checked={notificationsEnabled}
                onCheckedChange={setNotificationsEnabled}
              />
            </div>

            <Separator />

            <div className="flex items-center justify-between">
              <div>
                <p className="font-medium">Language</p>
                <p className="text-sm text-muted-foreground">Change app language</p>
              </div>
              <Badge variant="outline">{t('appName').includes('Namma') ? 'English' : 'Other'}</Badge>
            </div>
          </CardContent>
        </Card>

        {/* Data Management */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Download size={20} className="text-orange-600" />
              Data Management
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Button
              onClick={handleExportData}
              variant="outline"
              className="w-full justify-start"
            >
              <Download size={16} className="mr-2" />
              Export All Data (JSON)
            </Button>

            <Button
              variant="outline"
              className="w-full justify-start"
              disabled
            >
              <Upload size={16} className="mr-2" />
              Import Data (Coming Soon)
            </Button>

            <Separator />

            <Dialog>
              <DialogTrigger className="inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50 bg-destructive text-destructive-foreground shadow-sm hover:bg-destructive/90 h-9 px-4 py-2 w-full justify-start">
                <Trash2 size={16} className="mr-2" />
                Clear All Data
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Clear All Data?</DialogTitle>
                  <DialogDescription>
                    This will permanently delete all customers, transactions, and settings. This action cannot be undone. Make sure to export your data first!
                  </DialogDescription>
                </DialogHeader>
                <div className="flex gap-2 mt-4">
                  <Button variant="outline" className="flex-1">
                    Cancel
                  </Button>
                  <Button variant="destructive" className="flex-1" onClick={handleClearData}>
                    Yes, Clear All
                  </Button>
                </div>
              </DialogContent>
            </Dialog>
          </CardContent>
        </Card>

        {/* App Info */}
        <Card className="bg-gradient-to-br from-purple-50 to-pink-50 border-purple-200">
          <CardContent className="p-4 text-center">
            <p className="font-bold text-lg bg-gradient-to-r from-purple-600 to-pink-600 bg-clip-text text-transparent">
              {t('appName')}
            </p>
            <p className="text-sm text-muted-foreground mt-1">Version 1.0.0</p>
            <p className="text-xs text-muted-foreground mt-2">
              Digital Khata for Village Markets
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              Built with ❤️ for small vendors
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
