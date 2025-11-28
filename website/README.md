# ZekoChat Website

This is the official website for ZekoChat, providing information about the app, privacy policy, and account deletion requests.

## Files

- `index.html` - Homepage with app information and features
- `privacy.html` - Privacy Policy page
- `delete-account.html` - Account deletion request form
- `assets/logo.png` - App logo

## Features

- Responsive design using Tailwind CSS
- Mobile-friendly navigation
- Privacy Policy compliant with GDPR and other privacy regulations
- Account deletion request form for user data management
- Modern, clean UI matching the app's design language

## Deployment

To deploy this website:

1. Upload all files to your web server
2. Ensure the `assets` folder and `logo.png` are accessible
3. Configure your backend to handle the account deletion form submissions (currently the form just logs to console)

## Customization

- Update email addresses in `privacy.html` and `delete-account.html`
- Modify colors in the Tailwind config if needed
- Add your actual backend endpoint for form submission in `delete-account.html`

## Notes

- The account deletion form currently only shows a success message. You'll need to implement backend handling to process actual deletion requests.
- Update the Google Play Store link in `index.html` with your actual app URL once published.

