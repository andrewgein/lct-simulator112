export function matchesProfessionalProfile(profile, professionalProfile) {
    if (!professionalProfile?.trainingTrack) return true;
    if (profile?.trainingTrack !== professionalProfile.trainingTrack) return false;
    return professionalProfile.trainingTrack !== "DDS" || profile.ddsService === professionalProfile.ddsService;
}

export function filterStudentsByProfessionalProfile(students, professionalProfile) {
    return students.filter((student) => matchesProfessionalProfile(student.profile, professionalProfile));
}
