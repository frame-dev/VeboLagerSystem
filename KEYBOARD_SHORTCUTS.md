# Keyboard Shortcuts

This document lists the keyboard shortcuts available in the Swing GUIs of VEBO Lagersystem.

## Notes

- `Ctrl/Cmd` means `Ctrl` on Windows/Linux and `Cmd` on macOS.
- `Esc` closes the current window in the main desktop GUIs unless noted otherwise.
- Shortcuts that delete or remove entries only run when no text field is actively being edited.

## MainGUI

| Shortcut | Action |
| --- | --- |
| `Ctrl/Cmd+,` | Open `SettingsGUI` |
| `Ctrl/Cmd+Shift+N` | Open `NotesGUI` |
| `Ctrl/Cmd+Shift+C` | Open the converter for the selected article |
| `Ctrl/Cmd+1` | Open the `Artikel` tab |
| `Ctrl/Cmd+2` | Open the `Lieferanten` tab |
| `Ctrl/Cmd+3` | Open the `Bestellungen` tab |
| `Ctrl/Cmd+4` | Open the `Kunden` tab |
| `Ctrl/Cmd+5` | Open the `Lieferantenbestellungen` tab |
| `Ctrl/Cmd+6` | Open the `Protokolle` tab |

## ArticleGUI

| Shortcut | Action |
| --- | --- |
| `Esc` | Close window |
| `Ctrl/Cmd+F` | Focus article search |
| `Ctrl/Cmd+N` | Add article |
| `Ctrl/Cmd+E` | Edit selected article |
| `Delete` | Delete selected article |
| `F5` | Reload articles |
| `Ctrl/Cmd+L` | Clear search/filter text |

## ClientGUI

| Shortcut | Action |
| --- | --- |
| `Esc` | Close window |
| `Ctrl/Cmd+F` | Focus search |
| `Ctrl/Cmd+N` | Add client |
| `Ctrl/Cmd+E` | Edit selected client |
| `Delete` | Delete selected client |
| `F5` | Refresh client list |
| `Ctrl/Cmd+L` | Clear search/filter |

## VendorGUI

| Shortcut | Action |
| --- | --- |
| `Esc` | Close window |
| `Ctrl/Cmd+F` | Focus search |
| `Ctrl/Cmd+N` | Add vendor |
| `Ctrl/Cmd+E` | Edit selected vendor |
| `Delete` | Delete selected vendor |
| `F5` | Refresh vendor list |
| `Ctrl/Cmd+L` | Clear search/filter |
| `Ctrl/Cmd+Shift+A` | Open the full supplied-articles dialog for the selected vendor |

### Vendor Articles Dialog

| Shortcut | Action |
| --- | --- |
| `Esc` | Close dialog |
| Typing in search field | Filter the article list live |
| Double-click an entry | Copy the selected entry |

## OrderGUI

| Shortcut | Action |
| --- | --- |
| `Esc` | Close window |
| `Ctrl/Cmd+F` | Focus search |
| `Ctrl/Cmd+N` | Create a new order |
| `Ctrl/Cmd+E` | Edit selected order |
| `Delete` | Delete selected order |
| `Ctrl/Cmd+Shift+C` | Complete selected order |
| `F5` | Refresh order list |
| `Ctrl/Cmd+L` | Clear search/filter |

## NewOrderGUI

| Shortcut | Action |
| --- | --- |
| `Esc` | Close window |
| `Ctrl/Cmd+F` | Focus inline article search |
| `Ctrl/Cmd+Shift+A` | Add articles from `ArticleListGUI` |
| `Delete` | Remove selected order article |
| `Ctrl/Cmd+P` | Export the draft order as PDF |
| `Ctrl/Cmd+S` | Create/save the order |
| `F1` | Open help |

## SupplierOrderGUI

| Shortcut | Action |
| --- | --- |
| `Esc` | Close window |
| `Ctrl/Cmd+F` | Focus search |
| `Delete` | Remove selected supplier-order row |
| `Ctrl/Cmd+S` | Save supplier orders |
| `F5` | Refresh table |
| `Ctrl/Cmd+L` | Clear search |

## LogsGUI

| Shortcut | Action |
| --- | --- |
| `Esc` | Close window |
| `Ctrl/Cmd+F` | Focus log search |
| `Ctrl/Cmd+L` | Clear active filters |
| `Ctrl/Cmd+P` | Export filtered logs as PDF |
| `Ctrl/Cmd+Shift+C` | Export filtered logs as CSV |
| `F5` | Refresh current logs |
| `Ctrl/Cmd+1` | Show order logs |
| `Ctrl/Cmd+2` | Show supplier logs |
| `Ctrl/Cmd+3` | Show supplier-order entries |
| `Ctrl/Cmd+4` | Show all log sources |

## SettingsGUI

| Shortcut | Action |
| --- | --- |
| `Esc` | Close window |
| `Ctrl/Cmd+F` | Focus settings search |
| `Ctrl/Cmd+S` | Save settings |

## ArticleListGUI

| Shortcut | Action |
| --- | --- |
| `Ctrl+F` | Focus search |
| `Enter` | Edit selected quantity |
| `Delete` | Remove selected article |
| `Ctrl+L` | Clear all staged articles |
| `F5` | Refresh staged article list |
| `Esc` | Close window |

## NotesGUI

| Shortcut | Action |
| --- | --- |
| `Ctrl/Cmd+N` | Create a new note |
| `Ctrl/Cmd+F` | Focus search |
| `Delete` | Delete selected note |
| `F5` | Refresh notes |
| `Esc` | Close window |

## EditOrderGUI

| Shortcut | Action |
| --- | --- |
| `Esc` | Close window |
| `Ctrl/Cmd+S` | Save changes |

## CompleteOrderGUI

| Shortcut | Action |
| --- | --- |
| `Esc` | Close window |
| `F5` | Refresh open orders |
| `Ctrl/Cmd+Shift+C` | Complete the selected order |

## PartialOrderGUI

| Shortcut | Action |
| --- | --- |
| `Esc` | Close window |
| `Ctrl/Cmd+S` | Finish the partial-order completion |

## ConverterGUI

| Shortcut | Action |
| --- | --- |
| `Esc` | Close window |
| `Ctrl/Cmd+Enter` | Run calculation |
| `Ctrl/Cmd+Shift+A` | Add calculated result to the article list or return the filling value |

## Existing Dialog Shortcuts

These dialogs already provide their own keyboard shortcuts:

| Dialog | Shortcut | Action |
| --- | --- | --- |
| `ArticleDialog` | `Esc` | Close dialog |
| `ArticleDialog` | `Ctrl+Enter` | Confirm article form |
| `VendorDialog` | `Esc` | Close dialog |
| `ClientDialog` | `Esc` | Close dialog |
| `ArticleQrCodeDialog` | `Esc` | Close dialog |
| `ArticleQrCodeDialog` | `F5` | Refresh QR-code data |
| `ArticleQrPreviewDialog` | `Esc` | Close dialog |
| `ArticleQrPreviewDialog` | `Ctrl+S` | Save/export preview |
| `DisplayWarningDialog` | `Esc` | Close dialog |
| `ArticleStatsDialog` | `Esc` | Close dialog |
| `MessageDialog` | `Esc` | Close dialog |
