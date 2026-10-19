# In-App Contact Search & Dropdown Selection with Avatar Preview

Provides an instant, in-app contact search and auto-complete dropdown directly within name and phone input fields, eliminating the need to leave the app for external contact picker apps while displaying contact photos, phone numbers, and phone types.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during clarification and are incorporated into this implementation:

- **Confirmed Decision 1 (Search Scope)**: Both the **Name** and **Phone** input fields will support live contact search and dropdown suggestions.
- **Confirmed Decision 2 (Fallback Access)**: The existing contact book icon will be retained as an alternative action button for users who still want to use the system picker.
- **Confirmed Decision 3 (Multiple Numbers Handling)**: When a selected contact has multiple phone numbers, the app will prompt with a number selection dialog (`SelectContactPhoneDialog`) to ensure the user gets their desired phone line.

---

## 1. Overview & Core Concept

### What It Does
Currently, adding a debt/loan contact or vendor requires tapping an external contact icon, which opens the system-level contact picker outside the app. 

With this upgrade:
- Users can type directly into the **Name** or **Phone** field to instantly search phone contacts on-device.
- An elegant Material 3 dropdown appears beneath the field showing:
  - **Contact Profile Photo** (if set in phonebook, loaded smoothly via Coil; fallback styled monogram avatar if none exists)
  - **Contact Full Name**
  - **Primary Phone Number & Label** (e.g., Mobile, Home, Work in Bangla/English)
  - Multi-number badge indicator (e.g., "2 numbers" / "২টি নম্বর")
- Selecting any suggestion instantly fills both the **Name** and **Phone** input fields without ever leaving the screen.

### Target Audience & Value
- Financial record keepers, shop owners, and daily ledger users in Bangladesh.
- Dramatically accelerates recording transactions and debts by avoiding context switching and external app launch lag.

---

## 2. User Experience & Visual Design

### Key User Flows

```
[ User taps Name or Phone input ]
               │
               ▼
   Has READ_CONTACTS permission?
      ├── No  ──► Friendly one-tap permission request or graceful hint
      └── Yes ──► Real-time debounce query as user types
               │
               ▼
[ Material 3 Dropdown Suggestions Popup ]
  ┌────────────────────────────────────────────────────────┐
  │ [Photo/Avatar]  Rahim Ahmed            [Mobile]        │
  │                 +880 1712-345678       (2 numbers)     │
  ├────────────────────────────────────────────────────────┤
  │ [Photo/Avatar]  Karim Uddin            [Personal]      │
  │                 +880 1819-876543                       │
  └────────────────────────────────────────────────────────┘
               │
               ▼
[ User taps a Contact Item ]
      ├── Contact has 1 phone ─────► Instantly populates Name & Phone
      └── Contact has >1 phones ──► Opens SelectContactPhoneDialog -> Populates
```

### Visual Identity & Layout
- **Dropdown Design**: Clean Material 3 elevated menu/card (`tonalElevation = 6.dp`, rounded corners `14.dp`, subtle border) directly anchored beneath the text field.
- **Avatar Styling**: 40x40.dp circular avatar with Coil `AsyncImage` for contact photo thumbnail (`ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI`), with graceful fallback to a high-contrast circular initial avatar.
- **Typography & Hierarchy**:
  - Name: `titleSmall` / `bodyMedium` with bold weight.
  - Number: `bodySmall` with tabular numbers and subtle tint.
  - Type Badge: Compact pill chip (`labelSmall`, e.g., "Mobile" / "মোবাইল").

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct ContentResolver Query vs Background Caching**
  - *Chosen Approach*: Direct debounced queries on `Dispatchers.IO` with `limit = 20` using `ContactsContract.CommonDataKinds.Phone.CONTENT_URI`.
  - *Why*: Instant, zero database storage overhead, always in sync with user's phonebook additions or edits, low memory footprint.
- **Decision 2: Seamless Permission Flow**
  - *Chosen Approach*: When focusing or typing in the field without permission, show a gentle inline search hint or trigger permission request without interrupting manual typing. Users can always type custom names and numbers freely even if permission is denied.

---

## 4. Technical Architecture & Data Strategy

```
┌───────────────────────────────────────────────────────────────┐
│                       ContactUtils.kt                         │
│  - searchPhoneContacts(context, query): Flow/List<DeviceContact>│
│  - query: filters DISPLAY_NAME or NUMBER                       │
│  - projection: ID, NAME, NUMBER, TYPE, LABEL, PHOTO_URI        │
│  - groups multi-number contacts cleanly                       │
└──────────────────────────────┬────────────────────────────────┘
                               │
                               ▼
┌───────────────────────────────────────────────────────────────┐
│               ContactAutocompleteTextField.kt                 │
│  - OutlinedTextField with anchor                              │
│  - Live DropdownMenu / Popup suggestion list                  │
│  - Coil AsyncImage for contact photo thumbnails               │
│  - OnSelect: fills Name & Phone + triggers multi-phone dialog │
└──────────────────────────────┬────────────────────────────────┘
                               │
                               ▼
┌───────────────────────────────────────────────────────────────┐
│          DebtsScreen.kt / Contact Entry Forms                 │
│  - AddLoanEntryBottomSheet & AddLoanEntryDialog               │
│  - AddNewVendorBottomSheet & AddNewVendorDialog               │
│  - EditContactPhoneDialog & EditVendorPhoneDialog             │
└───────────────────────────────────────────────────────────────┘
```

### Verification & Testing
1. **Compilation Check**: Verify with `compile_applet` ensuring no missing imports or broken signatures.
2. **Permission Handling**: Ensure clean fallback if `READ_CONTACTS` is not granted.
3. **Multi-Number Flow**: Verify that selecting a contact with multiple numbers launches the phone chooser dialog.
4. **Photo Rendering**: Verify Coil thumbnail rendering and placeholder fallback.
