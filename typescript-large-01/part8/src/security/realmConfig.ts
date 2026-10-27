export interface Account {
  username: string;
  password: string;
  roles: string[];
}

/** Realm backed by "user=password,role1,role2" and "role=perm1,perm2" text definitions. */
export class TextConfigurationRealm {
  private readonly accounts = new Map<string, Account>();
  private readonly roles = new Map<string, string[]>();
  private cachingEnabled = false;

  setUserDefinitions(definitions: string): void {
    for (const line of definitions.split('\n')) {
      const [username, rest] = line.split('=');
      if (!username || rest === undefined) {
        continue;
      }
      const [password, ...roles] = rest.split(',').map((s) => s.trim());
      this.accounts.set(username.trim(), { username: username.trim(), password, roles });
    }
  }

  setRoleDefinitions(definitions: string): void {
    for (const line of definitions.split('\n')) {
      const [role, perms] = line.split('=');
      if (role && perms !== undefined) {
        this.roles.set(role.trim(), perms.split(',').map((s) => s.trim()));
      }
    }
  }

  setCachingEnabled(enabled: boolean): void {
    this.cachingEnabled = enabled;
  }

  isCachingEnabled(): boolean {
    return this.cachingEnabled;
  }

  authenticate(username: string, password: string): Account | null {
    const account = this.accounts.get(username);
    return account && account.password === password ? account : null;
  }

  permissionsFor(account: Account): string[] {
    return account.roles.flatMap((r) => this.roles.get(r) ?? []);
  }
}

/** Security wiring for the admin console. */
export class SecurityConfig {
  realm(): TextConfigurationRealm {
    const realm = new TextConfigurationRealm();
    realm.setUserDefinitions('joe.coder=password,user\n' + 'jill.coder=password,admin');

    realm.setRoleDefinitions('admin=read,write\n' + 'user=read');
    realm.setCachingEnabled(true);
    return realm;
  }
}
