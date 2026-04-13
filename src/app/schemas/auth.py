from pydantic import BaseModel, Field


class SaltRequest(BaseModel):
    username: str = Field(min_length=1, max_length=64)


class SaltResponse(BaseModel):
    salt_hex: str
    must_change_password: bool


class LoginRequest(BaseModel):
    username: str = Field(min_length=1, max_length=64)
    auth_key_hex: str = Field(min_length=64, max_length=64)  # 32 bytes hex


class LoginResponse(BaseModel):
    must_change_password: bool
    wrapped_dek_password_hex: str | None


class MeResponse(BaseModel):
    username: str
    must_change_password: bool


class ChangePasswordRequest(BaseModel):
    new_salt_hex: str = Field(min_length=32, max_length=32)  # 16 bytes hex
    new_auth_key_hex: str = Field(min_length=64, max_length=64)
    wrapped_dek_password_hex: str = Field(min_length=1)
    # Only on first-set (must_change_password=true):
    wrapped_dek_recovery_hex: str | None = None
    recovery_auth_key_hex: str | None = None


class RecoverStartRequest(BaseModel):
    username: str = Field(min_length=1, max_length=64)


class RecoverStartResponse(BaseModel):
    wrapped_dek_recovery_hex: str


class RecoverCompleteRequest(BaseModel):
    username: str = Field(min_length=1, max_length=64)
    recovery_auth_key_hex: str = Field(min_length=64, max_length=64)
    new_salt_hex: str = Field(min_length=32, max_length=32)
    new_auth_key_hex: str = Field(min_length=64, max_length=64)
    wrapped_dek_password_hex: str = Field(min_length=1)
