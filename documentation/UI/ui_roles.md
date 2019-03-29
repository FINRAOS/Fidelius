# User Roles

When a user logs into Fidelius, they are assigned a role that specifies what actions the user is able to perform in
Fidelius. Currently, there are three main permission roles for users

### Summary of Permissions for the User Roles

<table>
    <tr>
        <th></th>
        <th>Dev</th>
        <th>Ops</th>
        <th>Master</th>
    </tr>
    <tr>
        <td>Can view secrets information and history</td>
        <td>Yes, in any environment</td>
        <td>Yes, in any environment</td>
        <td>Yes, in any environment</td>
    </tr>
    <tr>
        <td>Can decrypt and view secrets</td>
        <td>Only on non-production tables</td>
        <td>Yes</td>
        <td>Yes</td>
    </tr>
    <tr>
        <td>Can add new secrets</td>
        <td>Only on non-production tables</td>
        <td>Yes</td>
        <td>Yes</td>
    </tr>
    <tr>
        <td>Can delete secrets</td>
        <td>Only on non-production tables</td>
        <td>Only on non-production tables</td>
        <td>Can delete any secret</td>
    </tr>
    <tr>
        <td>Can access any application/AGS</td>
        <td>No</td>
        <td>Yes</td>
        <td>Yes</td>
    </tr>
    <tr>
        <td>Can access a secrets table on any account and region</td>
        <td>Yes</td>
        <td>Yes</td>
        <td>Yes</td>
    </tr>
</table>