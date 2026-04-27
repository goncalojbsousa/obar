package com.obar.desktop.admin.sections.users;

import com.obar.bll.admin.AdminUserDTO;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.DetailPanelBinder.DetailViewModel;

import java.util.Locale;

/**
 * Maps an {@link AdminUserDTO} to the {@link DetailViewModel} consumed by
 * {@link com.obar.desktop.admin.shared.DetailPanelBinder}.
 *
 * <p>
 * Needs the ViewModel to resolve type-aware card values and labels,
 * avoiding the need to pass UserType separately.
 */
public final class UserDetailMapper {

    private UserDetailMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Builds a populated detail view model for the given user.
     */
    public static DetailViewModel from(AdminUserDTO user, UsersViewModel viewModel) {
        return new DetailViewModel.Builder()
                .initials(AdminFormatUtils.extractInitials(user.getName()))
                .title("Detalhe do " + viewModel.badgeLabel().toLowerCase(Locale.ROOT))
                .name(AdminFormatUtils.fallback(user.getName()))
                .email(AdminFormatUtils.fallback(user.getEmail()))
                .status(AdminFormatUtils.prettyStatus(user.getStatus()))
                .role(viewModel.badgeLabel())
                .phone(AdminFormatUtils.fallback(user.getPhone()))
                .created(user.getCreatedAt() == null ? "-" : user.getCreatedAt().toString())
                .card1(viewModel.detailCardOneTitle(), viewModel.cardOneValue(user))
                .card2(viewModel.detailCardTwoTitle(), viewModel.cardTwoValue(user))
                .card3(viewModel.detailCardThreeTitle(), viewModel.cardThreeValue(user))
                .card4(viewModel.detailCardFourTitle(), viewModel.cardFourValue(user))
                .ref(viewModel.referenceTitle(), viewModel.referenceValue(user))
                .build();
    }

    /**
     * Builds the empty/placeholder state shown when nothing is selected.
     */
    public static DetailViewModel empty(UsersViewModel viewModel) {
        return new DetailViewModel.Builder()
                .title(viewModel.sectionTitle())
                .role(viewModel.badgeLabel())
                .card1(viewModel.detailCardOneTitle(), "-")
                .card2(viewModel.detailCardTwoTitle(), "-")
                .card3(viewModel.detailCardThreeTitle(), "-")
                .card4(viewModel.detailCardFourTitle(), "-")
                .ref(viewModel.referenceTitle(), "-")
                .build();
    }
}
