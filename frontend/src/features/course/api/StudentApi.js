import { getAllUsers } from "../../auth/api/AdminUserApi";
import { getAllUserProfiles } from "../../profile/api/UserProfileApi";

export async function getStudents(token) {
    const [usersResponse, profilesResponse] = await Promise.all([
        getAllUsers(token),
        getAllUserProfiles(token)
    ]);
    if (!usersResponse.ok || !profilesResponse.ok) {
        throw new Error("Не удалось загрузить обучающихся");
    }
    const users = (await usersResponse.json()).data || [];
    const profiles = await profilesResponse.json();
    const profilesByUserId = new Map(profiles.map((profile) => [String(profile.userId), profile]));
    return users.filter((user) => user.role === "STUDENT").map((user) => ({
        ...user,
        profile: profilesByUserId.get(String(user.id)) || null
    }));
}
