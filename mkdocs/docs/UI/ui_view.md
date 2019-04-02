# Viewing Credentials

To view a credential, click on the icon with three vertical dots to the right of the credential in the credentials
table, then select the `View` option. This will display the View Credential sidebar. The View Credential sidebar
contains two tabs: the Information tab and the History tab.

## Information Tab
The Information tab displays all of the information on the credential, including its encrypted secret. By default, the 
credential's secret is encrypted and represented by a series of dots under the Secret section. If the user has proper 
permissions, the eye icon next to the dots can be clicked to decrypt the secret and display it in plaintext on the 
screen. A new icon, a series of overlapping squares, should appear. Clicking that icon will save the decrypted secret to
the clipboard.

![View Credential Information](../assets/view_credential.png)

## History Tab
The History tab displays a table of all of the edits made to a credential, including the editor's IAM role, when the
edit was made, and the revision number. Clicking on the column headers will sort the table based on the contents of that
column. This tab can be opened directly from the credentials table by clicking the vertical three dots icon and
selecting `History`.

![View Credential History](../assets/view_history.png)